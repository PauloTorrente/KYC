package com.donjuan.kyc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.textract.TextractClient;

import java.net.URI;

/**
 * O armazenamento dos documentos KYC (imagens/PDF) fica no Cloudflare R2, que fala a
 * mesma API do S3 (por isso reaproveitamos o SDK da AWS, so' apontando para o endpoint
 * do R2 com as credenciais do R2). O OCR (Textract) continua na AWS de verdade, pois
 * e' um servico que so' existe la' — por isso tem regiao e credenciais separadas.
 */
@Configuration
public class S3Config {

    @Value("${kyc.r2.account-id}")
    private String r2AccountId;

    @Value("${kyc.r2.access-key-id}")
    private String r2AccessKeyId;

    @Value("${kyc.r2.secret-access-key}")
    private String r2SecretAccessKey;

    @Value("${kyc.textract.region}")
    private String textractRegion;

    @Value("${kyc.textract.access-key-id:}")
    private String textractAccessKeyId;

    @Value("${kyc.textract.secret-access-key:}")
    private String textractSecretAccessKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(r2Endpoint())
                .region(Region.of("auto"))
                .credentialsProvider(r2Credentials())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(r2Endpoint())
                .region(Region.of("auto"))
                .credentialsProvider(r2Credentials())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    public TextractClient textractClient() {
        var builder = TextractClient.builder().region(Region.of(textractRegion));
        // O .env so' populate o Environment do Spring, nao as env vars reais do processo —
        // a cadeia padrao de credenciais da AWS (que le' System.getenv) nao enxerga isso em
        // dev local. Por isso, se a chave vier configurada, passamos explicitamente; em
        // producao com variaveis de ambiente reais ou IAM role, o SDK cai na cadeia padrao.
        if (!textractAccessKeyId.isBlank() && !textractSecretAccessKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(textractAccessKeyId, textractSecretAccessKey)));
        }
        return builder.build();
    }

    private URI r2Endpoint() {
        return URI.create("https://%s.r2.cloudflarestorage.com".formatted(r2AccountId));
    }

    private StaticCredentialsProvider r2Credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(r2AccessKeyId, r2SecretAccessKey));
    }
}
