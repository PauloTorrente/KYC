package com.donjuan.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClienteRequest {

    @NotBlank(message = "nome e obrigatorio")
    @Size(min = 2, max = 150, message = "nome deve ter entre 2 e 150 caracteres")
    private String nome;

    @NotBlank(message = "cpf e obrigatorio")
    private String cpf;

    /** opcional: identificador do operador que esta cadastrando */
    private String registradoPor;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
}
