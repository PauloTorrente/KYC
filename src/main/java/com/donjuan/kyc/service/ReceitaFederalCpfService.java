package com.donjuan.kyc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Validacao de CPF direto na base da Receita Federal, via Serpro (produto "Consulta
 * CPF", versao 3). Confirma se o CPF existe, esta' "Regular" (nem suspenso, cancelado,
 * nulo ou titular falecido) e se o nome bate com o que a Receita tem registrado.
 *
 * IMPORTANTE - diferente das outras integracoes deste projeto (Anthropic, AWS,
 * Cloudflare, OpenSanctions), essa NAO tem cadastro livre: exige certificado digital
 * e-CNPJ da empresa e contrato formal pela loja.serpro.gov.br antes de gerar as
 * credenciais (consumer key/secret). Ver .env.example para o passo a passo.
 *
 * Os detalhes tecnicos abaixo (endpoint, fluxo OAuth2 client_credentials) foram
 * montados com base na documentacao publica da Serpro (padrao usado em todas as APIs
 * deles, incluindo Datavalid e Receita Integra) e no anuncio oficial da versao 3 da
 * Consulta CPF — o portal de documentacao detalhada deles e' uma SPA que exige sessao
 * autenticada pra abrir as paginas de referencia completas. Ao contratar, a Serpro
 * envia um "Manual de Informacoes Tecnicas" com os detalhes definitivos; confira TOKEN_URL
 * e API_URL contra esse manual antes de usar em producao, caso algo tenha mudado.
 */
@Service
public class ReceitaFederalCpfService {

    private static final String TOKEN_URL = "https://gateway.apiserpro.serpro.gov.br/token";
    private static final String API_URL = "https://apicpf.estaleiro.serpro.gov.br/api/v3/cpf/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${kyc.serpro.consumer-key:}")
    private String consumerKey;

    @Value("${kyc.serpro.consumer-secret:}")
    private String consumerSecret;

    private volatile String tokenCache;
    private volatile Instant tokenExpiraEm = Instant.EPOCH;

    public static class ReceitaFederalException extends RuntimeException {
        public ReceitaFederalException(String m) { super(m); }
        public ReceitaFederalException(String m, Throwable c) { super(m, c); }
    }

    public boolean configurado() {
        return consumerKey != null && !consumerKey.isBlank() && consumerSecret != null && !consumerSecret.isBlank();
    }

    private synchronized String token() {
        if (!configurado()) {
            throw new ReceitaFederalException(
                    "validacao de CPF (Receita Federal/Serpro) nao configurada (defina SERPRO_CONSUMER_KEY e SERPRO_CONSUMER_SECRET)");
        }
        if (tokenCache != null && Instant.now().isBefore(tokenExpiraEm)) {
            return tokenCache;
        }

        String credenciais = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + credenciais)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ReceitaFederalException("falha ao autenticar com a Serpro", e);
        }
        if (response.statusCode() >= 400) {
            throw new ReceitaFederalException("Serpro recusou a autenticacao (status " + response.statusCode() + ")");
        }

        try {
            JsonNode body = MAPPER.readTree(response.body());
            String accessToken = body.path("access_token").asText(null);
            if (accessToken == null) {
                throw new ReceitaFederalException("resposta de token da Serpro sem access_token");
            }
            int expiresIn = body.path("expires_in").asInt(3300);
            tokenCache = accessToken;
            tokenExpiraEm = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
            return accessToken;
        } catch (ReceitaFederalException e) {
            throw e;
        } catch (Exception e) {
            throw new ReceitaFederalException("falha ao interpretar resposta de token da Serpro", e);
        }
    }

    /** Consulta o CPF na base da Receita Federal. A v3 da API exige CPF + data de nascimento. */
    public JsonNode consultar(String cpf, LocalDate dataNascimento) {
        if (cpf == null || cpf.isBlank()) {
            throw new ReceitaFederalException("CPF obrigatorio pra consulta");
        }
        if (dataNascimento == null) {
            throw new ReceitaFederalException("data de nascimento obrigatoria pra consulta (exigencia da Receita Federal)");
        }
        String cpfLimpo = cpf.replaceAll("\\D", "");
        String dataFormatada = dataNascimento.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + cpfLimpo + "?dataNascimento=" + dataFormatada))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token())
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ReceitaFederalException("falha ao consultar o CPF na Serpro", e);
        }

        if (response.statusCode() == 404) {
            throw new ReceitaFederalException("CPF nao encontrado na base da Receita Federal");
        }
        if (response.statusCode() >= 400) {
            throw new ReceitaFederalException("Serpro retornou status " + response.statusCode() + " ao consultar o CPF");
        }

        try {
            return MAPPER.readTree(response.body());
        } catch (Exception e) {
            throw new ReceitaFederalException("falha ao interpretar resposta da consulta de CPF", e);
        }
    }
}
