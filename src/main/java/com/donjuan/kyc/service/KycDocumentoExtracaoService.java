package com.donjuan.kyc.service;

import com.donjuan.kyc.dto.kyc.KycExtracaoResponse;
import com.donjuan.kyc.model.KycDocumento;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeIdRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeIdResponse;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.IdentityDocumentField;

import java.util.HashMap;
import java.util.Map;

/**
 * Extrai campos de documentos de identidade via AWS Textract (AnalyzeID).
 * O arquivo mora no Cloudflare R2 (nao num bucket S3 da AWS), entao o Textract nao
 * consegue ler por referencia bucket/key: baixamos os bytes do storage e mandamos
 * direto na chamada. A API foi treinada para CNH/passaporte dos EUA: em CPF
 * brasileiro a precisao varia, por isso o resultado deve ser tratado como
 * sugestao, nao como verdade absoluta.
 */
@Service
public class KycDocumentoExtracaoService {

    private final TextractClient textractClient;
    private final S3StorageService storage;

    public KycDocumentoExtracaoService(TextractClient textractClient, S3StorageService storage) {
        this.textractClient = textractClient;
        this.storage = storage;
    }

    public static class ExtracaoException extends RuntimeException {
        public ExtracaoException(String m) { super(m); }
        public ExtracaoException(String m, Throwable c) { super(m, c); }
    }

    public KycExtracaoResponse extrair(KycDocumento documento) {
        return extrairDeBytes(storage.download(documento.getStorageKey()));
    }

    /**
     * Mesma extracao, mas direto a partir dos bytes do arquivo — usado pra ler um
     * documento ainda nao associado a um cliente (ex.: preencher o formulario de
     * cadastro automaticamente antes de salvar).
     */
    public KycExtracaoResponse extrairDeBytes(byte[] arquivo) {
        AnalyzeIdRequest request = AnalyzeIdRequest.builder()
                .documentPages(Document.builder().bytes(SdkBytes.fromByteArray(arquivo)).build())
                .build();

        AnalyzeIdResponse response;
        try {
            response = textractClient.analyzeID(request);
        } catch (SdkException e) {
            throw new ExtracaoException("falha ao extrair dados do documento", e);
        }

        if (response.identityDocuments().isEmpty()) {
            throw new ExtracaoException("nenhum documento reconhecido na imagem");
        }

        Map<String, String> campos = new HashMap<>();
        for (IdentityDocumentField field : response.identityDocuments().get(0).identityDocumentFields()) {
            String tipo = field.type() != null ? field.type().text() : null;
            String valor = field.valueDetection() != null ? field.valueDetection().text() : null;
            if (tipo != null && valor != null && !valor.isBlank()) {
                campos.put(tipo, valor);
            }
        }

        return KycExtracaoResponse.from(campos);
    }
}
