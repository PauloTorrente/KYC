package com.donjuan.kyc.dto.kyc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Resultado da segunda camada de verificacao (Claude), cruzando o cadastro do cliente,
 * o texto lido por OCR (Textract) e a imagem do documento. E' sempre uma sugestao para
 * apoiar o operador humano na decisao — nunca aprova/reprova nem altera o cadastro sozinha.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycVerificacaoIaResponse {
    public boolean consistente;
    public String risco;
    public List<String> divergencias;
    public List<String> sinaisDeAlerta;
    public String parecer;
}
