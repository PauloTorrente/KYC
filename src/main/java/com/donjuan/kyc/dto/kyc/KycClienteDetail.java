package com.donjuan.kyc.dto.kyc;

import com.donjuan.kyc.model.KycMaster;
import com.donjuan.kyc.service.KycMaskUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cliente completo (GET /api/kyc/clientes/:id). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycClienteDetail {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Integer id;
    public String codigo;
    public String nome;
    public String documento;
    public String numero;
    public String nacionalidade;
    public String telefone;
    public String extra;
    public String plataforma;
    public String origem;
    public String estadoKyc;
    public LocalDate dataUltimaRevisao;
    public String enderecoCompleto;
    public String cidade;
    public String estadoProvincia;
    public String cepPostal;
    public String paisFiscal;
    public String documentoVerificado;
    public String selfieVerificada;
    public String comprovanteResidencia;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDate dataCadastro;
    public String cnhRegistro;
    public String cnhValidade;
    public String cnhEmissao;
    public String cnhPrimeiraHabilitacao;
    public String filiacao;
    public LocalDate dataNascimento;
    public String indicadoPor;
    public String confianca;
    public String confiancaLabel;
    public JsonNode profit;

    public static KycClienteDetail from(KycMaster c) {
        KycClienteDetail r = new KycClienteDetail();
        r.id = c.getId();
        r.codigo = c.getCodigo();
        r.nome = c.getNombre();
        r.documento = c.getDocumento();
        r.numero = KycMaskUtil.maskDocumento(c.getNumero());
        r.nacionalidade = c.getNacionalidade();
        r.telefone = KycMaskUtil.maskTelefone(c.getCodigo(), c.getTelefone());
        r.extra = c.getExtra();
        r.plataforma = c.getPlataforma();
        r.origem = c.getOrigem() != null ? c.getOrigem() : KycMaskUtil.origem(c.getPlataforma());
        r.estadoKyc = c.getEstadoKyc();
        r.dataUltimaRevisao = c.getDataUltimaRevisao();
        r.enderecoCompleto = c.getEnderecoCompleto();
        r.cidade = c.getCidade();
        r.estadoProvincia = c.getEstadoProvincia();
        r.cepPostal = c.getCepPostal();
        r.paisFiscal = c.getPaisFiscal();
        r.documentoVerificado = c.getDocumentoVerificado();
        r.selfieVerificada = c.getSelfieVerificada();
        r.comprovanteResidencia = c.getComprovanteResidencia();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        r.dataCadastro = c.getDataCadastro();
        r.cnhRegistro = c.getCnhRegistro();
        r.cnhValidade = c.getCnhValidade();
        r.cnhEmissao = c.getCnhEmissao();
        r.cnhPrimeiraHabilitacao = c.getCnhPrimeiraHabilitacao();
        r.filiacao = c.getFiliacao();
        r.dataNascimento = c.getDataNascimento();
        r.indicadoPor = c.getIndicadoPor();
        r.confianca = c.getConfianca() != null ? c.getConfianca() : KycMaskUtil.confianca(c.getPlataforma());
        r.confiancaLabel = c.getConfiancaLabel() != null ? c.getConfiancaLabel() : KycMaskUtil.confiancaLabel(r.confianca);
        if (c.getProfit() != null) {
            try {
                r.profit = MAPPER.readTree(c.getProfit());
            } catch (Exception ignored) {
                r.profit = null;
            }
        }
        return r;
    }
}
