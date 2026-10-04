package com.donjuan.kyc.dto.kyc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/** Sugestao de dados lidos do documento via OCR — sempre precisa de revisao humana. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycExtracaoResponse {
    public String nome;
    public String documento;
    public String dataNascimento;
    public String endereco;
    public Map<String, String> camposBrutos;
    /** Preenchido so' quando o resultado passou pela conferencia do Claude (ver corrigirExtracao). */
    public List<String> correcoesIa;

    public static KycExtracaoResponse from(Map<String, String> campos) {
        KycExtracaoResponse r = new KycExtracaoResponse();

        String primeiro = campos.get("FIRST_NAME");
        String ultimo = campos.get("LAST_NAME");
        String nome = ((primeiro != null ? primeiro : "") + " " + (ultimo != null ? ultimo : "")).trim();
        r.nome = nome.isBlank() ? null : nome;

        r.documento = campos.get("DOCUMENT_NUMBER");
        r.dataNascimento = campos.get("DATE_OF_BIRTH");
        r.endereco = campos.get("ADDRESS");
        r.camposBrutos = campos;
        return r;
    }
}
