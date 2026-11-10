package com.donjuan.kyc.dto.kyc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Resultado da validacao do CPF direto na base da Receita Federal (via Serpro).
 * Confirma existencia, situacao cadastral e conferencia do nome — nao e' antecedente
 * criminal nem score de risco, so' confirma que o CPF e' real e os dados batem.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycValidacaoCpfResponse {
    public boolean consistente;
    public String situacaoCadastral;
    public String nomeReceita;
    public String parecer;
}
