package com.donjuan.kyc.dto.kyc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Resultado da checagem contra listas restritivas (sancoes OFAC/ONU/UE e PEP), via
 * OpenSanctions. Sempre uma sugestao pro operador revisar — nome parecido nao significa
 * que e' a mesma pessoa, nunca e' uma decisao automatica de bloqueio.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycListaRestritivaResponse {
    public boolean possivelMatch;
    public List<Resultado> resultados;
    public String parecer;

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Resultado {
        public String nome;
        public double score;
        public List<String> listas;
        public List<String> topicos;
    }
}
