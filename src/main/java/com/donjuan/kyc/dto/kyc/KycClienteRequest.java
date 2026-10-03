package com.donjuan.kyc.dto.kyc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/** Corpo de POST /api/kyc/clientes (inclui campos de CNH). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KycClienteRequest {

    @NotBlank(message = "nome e obrigatorio")
    private String nome;

    private String documento;
    private String numero;
    private String nacionalidade;
    private String telefone;
    private String extra;

    @NotBlank(message = "plataforma e obrigatoria")
    private String plataforma;

    @Pattern(regexp = "RESOLVIDO|PENDENTE|BLOQUEADO", message = "estado_kyc deve ser RESOLVIDO, PENDENTE ou BLOQUEADO")
    private String estadoKyc;
    private String enderecoCompleto;
    private String cidade;
    private String estadoProvincia;
    private String cepPostal;
    private String paisFiscal;
    private String documentoVerificado;
    private String selfieVerificada;
    private String comprovanteResidencia;
    private String indicadoPor;

    // colunas de CNH pedidas na especificacao
    private LocalDate dataCadastro;
    private String cnhRegistro;
    private String cnhValidade;
    private String cnhEmissao;
    private String cnhPrimeiraHabilitacao;
    private String filiacao;
    private LocalDate dataNascimento;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String nacionalidade) { this.nacionalidade = nacionalidade; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }
    public String getEstadoKyc() { return estadoKyc; }
    public void setEstadoKyc(String estadoKyc) { this.estadoKyc = estadoKyc; }
    public String getEnderecoCompleto() { return enderecoCompleto; }
    public void setEnderecoCompleto(String enderecoCompleto) { this.enderecoCompleto = enderecoCompleto; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstadoProvincia() { return estadoProvincia; }
    public void setEstadoProvincia(String estadoProvincia) { this.estadoProvincia = estadoProvincia; }
    public String getCepPostal() { return cepPostal; }
    public void setCepPostal(String cepPostal) { this.cepPostal = cepPostal; }
    public String getPaisFiscal() { return paisFiscal; }
    public void setPaisFiscal(String paisFiscal) { this.paisFiscal = paisFiscal; }
    public String getDocumentoVerificado() { return documentoVerificado; }
    public void setDocumentoVerificado(String documentoVerificado) { this.documentoVerificado = documentoVerificado; }
    public String getSelfieVerificada() { return selfieVerificada; }
    public void setSelfieVerificada(String selfieVerificada) { this.selfieVerificada = selfieVerificada; }
    public String getComprovanteResidencia() { return comprovanteResidencia; }
    public void setComprovanteResidencia(String comprovanteResidencia) { this.comprovanteResidencia = comprovanteResidencia; }
    public String getIndicadoPor() { return indicadoPor; }
    public void setIndicadoPor(String indicadoPor) { this.indicadoPor = indicadoPor; }
    public LocalDate getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDate dataCadastro) { this.dataCadastro = dataCadastro; }
    public String getCnhRegistro() { return cnhRegistro; }
    public void setCnhRegistro(String cnhRegistro) { this.cnhRegistro = cnhRegistro; }
    public String getCnhValidade() { return cnhValidade; }
    public void setCnhValidade(String cnhValidade) { this.cnhValidade = cnhValidade; }
    public String getCnhEmissao() { return cnhEmissao; }
    public void setCnhEmissao(String cnhEmissao) { this.cnhEmissao = cnhEmissao; }
    public String getCnhPrimeiraHabilitacao() { return cnhPrimeiraHabilitacao; }
    public void setCnhPrimeiraHabilitacao(String cnhPrimeiraHabilitacao) { this.cnhPrimeiraHabilitacao = cnhPrimeiraHabilitacao; }
    public String getFiliacao() { return filiacao; }
    public void setFiliacao(String filiacao) { this.filiacao = filiacao; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
}
