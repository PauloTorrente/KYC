package com.donjuan.kyc.dto.kyc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Pattern;

/** Corpo de PUT /api/kyc/clientes/:id/status. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycStatusUpdateRequest {

    @Pattern(regexp = "RESOLVIDO|PENDENTE|BLOQUEADO", message = "estado_kyc deve ser RESOLVIDO, PENDENTE ou BLOQUEADO")
    private String estadoKyc;

    public String getEstadoKyc() { return estadoKyc; }
    public void setEstadoKyc(String estadoKyc) { this.estadoKyc = estadoKyc; }
}
