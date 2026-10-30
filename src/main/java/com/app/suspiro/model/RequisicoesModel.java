package com.app.suspiro.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.app.suspiro.enums.StatusRequisicao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "requisicoes")
public class RequisicoesModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String protocolo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PacientesModel paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private UsuariosModel solicitante;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(length = 500)
    private String observacao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRequisicao status = StatusRequisicao.PENDENTE;

    @OneToOne(mappedBy = "requisicao")
    private AgendamentosModel agendamento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "notificacao_status_enviado", length = 20)
    private StatusRequisicao notificacaoStatusEnviado;

    @Column(name = "notificacao_tentativa_em")
    private LocalDateTime notificacaoTentativaEm;

    @Column(name = "notificacao_tentativas", nullable = false)
    private int notificacaoTentativas;

    @Column(name = "lembrete_pendente_enviado_em")
    private LocalDateTime lembretePendenteEnviadoEm;

    @Column(name = "lembrete_pendente_tentativa_em")
    private LocalDateTime lembretePendenteTentativaEm;

    @Column(name = "lembrete_pendente_tentativas", nullable = false)
    private int lembretePendenteTentativas;

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

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public PacientesModel getPaciente() {
        return paciente;
    }

    public void setPaciente(PacientesModel paciente) {
        this.paciente = paciente;
    }

    public UsuariosModel getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(UsuariosModel solicitante) {
        this.solicitante = solicitante;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public StatusRequisicao getStatus() {
        return status;
    }

    public void setStatus(StatusRequisicao status) {
        if (this.status != status) {
            this.notificacaoTentativaEm = null;
            this.notificacaoTentativas = 0;
        }
        this.status = status;
    }

    public StatusRequisicao getNotificacaoStatusEnviado() { return notificacaoStatusEnviado; }
    public void setNotificacaoStatusEnviado(StatusRequisicao value) { this.notificacaoStatusEnviado = value; }
    public LocalDateTime getNotificacaoTentativaEm() { return notificacaoTentativaEm; }
    public void setNotificacaoTentativaEm(LocalDateTime value) { this.notificacaoTentativaEm = value; }
    public int getNotificacaoTentativas() { return notificacaoTentativas; }
    public void incrementarNotificacaoTentativas() { this.notificacaoTentativas++; }
    public void limparTentativasNotificacao() { this.notificacaoTentativaEm = null; this.notificacaoTentativas = 0; }
    public LocalDateTime getLembretePendenteEnviadoEm() { return lembretePendenteEnviadoEm; }
    public void setLembretePendenteEnviadoEm(LocalDateTime value) { this.lembretePendenteEnviadoEm = value; }
    public LocalDateTime getLembretePendenteTentativaEm() { return lembretePendenteTentativaEm; }
    public void setLembretePendenteTentativaEm(LocalDateTime value) { this.lembretePendenteTentativaEm = value; }
    public int getLembretePendenteTentativas() { return lembretePendenteTentativas; }
    public void incrementarLembretePendenteTentativas() { this.lembretePendenteTentativas++; }
    public void limparTentativasLembretePendente() { this.lembretePendenteTentativaEm = null; this.lembretePendenteTentativas = 0; }

    public AgendamentosModel getAgendamento() {
        return agendamento;
    }

    public void setAgendamento(AgendamentosModel agendamento) {
        this.agendamento = agendamento;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
