package com.donjuan.kyc.dto.kyc;

import com.donjuan.kyc.model.KycDocumento;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;

/** Metadados de um documento KYC (nunca inclui a storage_key ou URL direta). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycDocumentoResponse {
    public Integer id;
    public String tipoDocumento;
    public String contentType;
    public Long tamanhoBytes;
    public LocalDateTime uploadedAt;

    public static KycDocumentoResponse from(KycDocumento d) {
        KycDocumentoResponse r = new KycDocumentoResponse();
        r.id = d.getId();
        r.tipoDocumento = d.getTipoDocumento();
        r.contentType = d.getContentType();
        r.tamanhoBytes = d.getTamanhoBytes();
        r.uploadedAt = d.getUploadedAt();
        return r;
    }
}
