package com.donjuan.kyc.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_master")
public class KycMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "documento", length = 20)
    private String documento;

    @Column(name = "numero", length = 50)
    private String numero;

    @Column(name = "nacionalidade", length = 50)
    private String nacionalidade;

    @Column(name = "telefone", length = 50)
    private String telefone;

    @Column(name = "extra")
    private String extra;

    @Column(name = "plataforma", length = 50)
    private String plataforma;

    @Column(name = "estado_kyc", length = 20)
    private String estadoKyc;

    @Column(name = "data_ultima_revisao")
    private LocalDate dataUltimaRevisao;

    @Column(name = "endereco_completo")
    private String enderecoCompleto;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "estado_provincia", length = 50)
    private String estadoProvincia;

    @Column(name = "cep_postal", length = 20)
    private String cepPostal;

    @Column(name = "pais_fiscal", length = 50)
    private String paisFiscal;

    @Column(name = "documento_verificado", length = 3)
    private String documentoVerificado;

    @Column(name = "selfie_verificada", length = 3)
    private String selfieVerificada;

    @Column(name = "comprovante_residencia", length = 3)
    private String comprovanteResidencia;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "data_cadastro")
    private LocalDate dataCadastro;

    @Column(name = "cnh_registro", length = 50)
    private String cnhRegistro;

    @Column(name = "cnh_validade", length = 20)
    private String cnhValidade;

    @Column(name = "cnh_emissao", length = 20)
    private String cnhEmissao;

    @Column(name = "cnh_primeira_habilitacao", length = 20)
    private String cnhPrimeiraHabilitacao;

    @Column(name = "filiacao")
    private String filiacao;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "indicado_por", length = 255)
    private String indicadoPor;

    @Column(name = "confianca", length = 20)
    private String confianca;

    @Column(name = "confianca_label", length = 50)
    private String confiancaLabel;

    @Column(name = "origem", length = 50)
    private String origem;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profit", columnDefinition = "jsonb")
    private String profit;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // getters / setters

    public Integer getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

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

    public LocalDate getDataUltimaRevisao() { return dataUltimaRevisao; }
    public void setDataUltimaRevisao(LocalDate dataUltimaRevisao) { this.dataUltimaRevisao = dataUltimaRevisao; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

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

    public String getIndicadoPor() { return indicadoPor; }
    public void setIndicadoPor(String indicadoPor) { this.indicadoPor = indicadoPor; }

    public String getConfianca() { return confianca; }
    public void setConfianca(String confianca) { this.confianca = confianca; }

    public String getConfiancaLabel() { return confiancaLabel; }
    public void setConfiancaLabel(String confiancaLabel) { this.confiancaLabel = confiancaLabel; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getProfit() { return profit; }
    public void setProfit(String profit) { this.profit = profit; }
}
