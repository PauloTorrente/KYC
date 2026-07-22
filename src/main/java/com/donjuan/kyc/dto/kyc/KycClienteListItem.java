package com.donjuan.kyc.dto.kyc;

import com.donjuan.kyc.model.KycMaster;
import com.donjuan.kyc.service.KycMaskUtil;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Linha da tabela de clientes (GET /api/kyc/clientes). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycClienteListItem {
    public Integer id;
    public String codigo;
    public String nome;
    public String documento;
    public String numero;
    public String telefone;
    public String plataforma;
    public String origem;
    public String estadoKyc;
    public String confianca;
    public String confiancaLabel;

    public static KycClienteListItem from(KycMaster c) {
        KycClienteListItem r = new KycClienteListItem();
        r.id = c.getId();
        r.codigo = c.getCodigo();
        r.nome = c.getNombre();
        r.documento = c.getDocumento();
        r.numero = KycMaskUtil.maskDocumento(c.getNumero());
        r.telefone = KycMaskUtil.maskTelefone(c.getCodigo(), c.getTelefone());
        r.plataforma = c.getPlataforma();
        r.origem = c.getOrigem() != null ? c.getOrigem() : KycMaskUtil.origem(c.getPlataforma());
        r.estadoKyc = c.getEstadoKyc();
        r.confianca = c.getConfianca() != null ? c.getConfianca() : KycMaskUtil.confianca(c.getPlataforma());
        r.confiancaLabel = c.getConfiancaLabel() != null ? c.getConfiancaLabel() : KycMaskUtil.confiancaLabel(r.confianca);
        return r;
    }
}
