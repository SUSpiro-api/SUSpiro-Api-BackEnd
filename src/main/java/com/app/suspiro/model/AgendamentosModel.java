package com.app.suspiro.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "agendamentos")
public class AgendamentosModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "requisicao_id", nullable = false, unique = true)
    private RequisicoesModel requisicao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PacientesModel paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agendado_por_id", nullable = false)
    private UsuariosModel agendadoPor;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, length = 150)
    private String local;

    @Column(length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RequisicoesModel getRequisicao() {
        return requisicao;
    }

    public void setRequisicao(RequisicoesModel requisicao) {
        this.requisicao = requisicao;
    }

    public PacientesModel getPaciente() {
        return paciente;
    }

    public void setPaciente(PacientesModel paciente) {
        this.paciente = paciente;
    }

    public UsuariosModel getAgendadoPor() {
        return agendadoPor;
    }

    public void setAgendadoPor(UsuariosModel agendadoPor) {
        this.agendadoPor = agendadoPor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
