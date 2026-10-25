package com.app.suspiro.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.suspiro.enums.Perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class UsuariosModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 255, unique = true)
    private String email;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 255)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "senha_provisoria", nullable = false)
    private boolean senhaProvisoria = false;

    @Column(name = "paciente_id")
    private UUID pacienteId;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion = 0;

    @Column(name = "token_redefinicao_senha_hash", length = 64)
    private String tokenRedefinicaoSenhaHash;

    @Column(name = "token_redefinicao_senha_expira_em")
    private LocalDateTime tokenRedefinicaoSenhaExpiraEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

       public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
    public String getEmail() {
    return email;
    }

    public void setEmail(String email) {
    this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public UUID getPacienteId() { return pacienteId; }

    public void setPacienteId(UUID pacienteId) { this.pacienteId = pacienteId; }

    public long getTokenVersion() { return tokenVersion; }

    public void incrementarTokenVersion() { this.tokenVersion++; }

    public boolean isSenhaProvisoria() {
        return senhaProvisoria;
    }

    public void setSenhaProvisoria(boolean senhaProvisoria) {
        this.senhaProvisoria = senhaProvisoria;
    }

    public String getTokenRedefinicaoSenhaHash() {
        return tokenRedefinicaoSenhaHash;
    }

    public void setTokenRedefinicaoSenhaHash(String tokenRedefinicaoSenhaHash) {
        this.tokenRedefinicaoSenhaHash = tokenRedefinicaoSenhaHash;
    }

    public LocalDateTime getTokenRedefinicaoSenhaExpiraEm() {
        return tokenRedefinicaoSenhaExpiraEm;
    }

    public void setTokenRedefinicaoSenhaExpiraEm(LocalDateTime tokenRedefinicaoSenhaExpiraEm) {
        this.tokenRedefinicaoSenhaExpiraEm = tokenRedefinicaoSenhaExpiraEm;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
