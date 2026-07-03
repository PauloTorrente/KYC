package com.donjuan.kyc.dto;

import com.donjuan.kyc.model.Cliente;
import java.time.Instant;

public class ClienteResponse {
    public Long id;
    public String nome;
    public String cpf;          // mascarado
    public Instant criadoEm;
    public String registradoPor;

    public static ClienteResponse from(Cliente c) {
        ClienteResponse r = new ClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cpf = c.getCpfMascarado();
        r.criadoEm = c.getCriadoEm();
        r.registradoPor = c.getRegistradoPor();
        return r;
    }
}
