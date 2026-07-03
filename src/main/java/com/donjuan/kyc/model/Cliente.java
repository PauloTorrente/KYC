package com.donjuan.kyc.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "cliente",
    uniqueConstraints = @UniqueConstraint(name = "uk_cliente_cpf_hash", columnNames = "cpf_hash")
)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    /**
     * Hash SHA-256 do CPF (com pepper). Permite checar duplicidade e buscar
     * sem guardar o numero em texto puro. NAO e reversivel.
     */
    @Column(name = "cpf_hash", nullable = false, length = 64)
    private String cpfHash;

    /** Versao mascarada apenas para exibicao: 123.***.***-09 */
    @Column(name = "cpf_mascarado", nullable = false, length = 14)
    private String cpfMascarado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /** Quem registrou (operador). Trilha minima de auditoria. */
    @Column(name = "registrado_por", length = 80)
    private String registradoPor;

    @PrePersist
    void onCreate() {
        if (criadoEm == null) criadoEm = Instant.now();
    }

    // getters / setters
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpfHash() { return cpfHash; }
    public void setCpfHash(String cpfHash) { this.cpfHash = cpfHash; }
    public String getCpfMascarado() { return cpfMascarado; }
    public void setCpfMascarado(String cpfMascarado) { this.cpfMascarado = cpfMascarado; }
    public Instant getCriadoEm() { return criadoEm; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
}
