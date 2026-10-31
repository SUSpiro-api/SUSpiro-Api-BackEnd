package com.app.suspiro.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import com.app.suspiro.enums.PrioridadeGuia;
import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.enums.TipoGuia;

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
@Table(name = "guias")
public class GuiaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String protocolo;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "paciente_nome", nullable = false, length = 150)
    private String pacienteNome;

    @Column(name = "paciente_cpf", nullable = false, length = 11)
    private String pacienteCpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoGuia tipo;

    @Column(length = 120)
    private String especialidade;

    @Column(length = 255)
    private String detalhe;

    @Column(name = "data_solicitada", nullable = false)
    private LocalDate dataSolicitada;

    @Column(length = 1000)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadeGuia prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusGuia status;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(name = "solicitante_nome", nullable = false, length = 150)
    private String solicitanteNome;

    @Column(name = "estabelecimento_nome", length = 180)
    private String estabelecimentoNome;

    @Column(name = "estabelecimento_endereco", length = 300)
    private String estabelecimentoEndereco;

    @Column(name = "estabelecimento_cidade", length = 120)
    private String estabelecimentoCidade;

    @Column(name = "estabelecimento_estado", length = 2)
    private String estabelecimentoEstado;

    @Column(name = "data_agendamento")
    private LocalDate dataAgendamento;

    @Column(name = "hora_agendamento")
    private LocalTime horaAgendamento;

    @Column(name = "agendado_por_id")
    private Long agendadoPorId;

    @Column(name = "agendado_por_nome", length = 150)
    private String agendadoPorNome;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "lembrete_pendente_enviado_em")
    private LocalDateTime lembretePendenteEnviadoEm;

    @Column(name = "notificacao_agendamento_enviada_em")
    private LocalDateTime notificacaoAgendamentoEnviadaEm;

    @Column(name = "lembrete_pendente_tentativa_em")
    private LocalDateTime lembretePendenteTentativaEm;

    @Column(name = "lembrete_pendente_tentativas", nullable = false)
    private int lembretePendenteTentativas;

    @Column(name = "notificacao_agendamento_tentativa_em")
    private LocalDateTime notificacaoAgendamentoTentativaEm;

    @Column(name = "notificacao_agendamento_tentativas", nullable = false)
    private int notificacaoAgendamentoTentativas;

    @PrePersist
    protected void onCreate() {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    public Long getId() { return id; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public String getEstabelecimentoNome() { return estabelecimentoNome; }
    public void setEstabelecimentoNome(String estabelecimentoNome) { this.estabelecimentoNome = estabelecimentoNome; }
    public String getEstabelecimentoEndereco() { return estabelecimentoEndereco; }
    public void setEstabelecimentoEndereco(String estabelecimentoEndereco) { this.estabelecimentoEndereco = estabelecimentoEndereco; }
    public String getEstabelecimentoCidade() { return estabelecimentoCidade; }
    public void setEstabelecimentoCidade(String estabelecimentoCidade) { this.estabelecimentoCidade = estabelecimentoCidade; }
    public String getEstabelecimentoEstado() { return estabelecimentoEstado; }
    public void setEstabelecimentoEstado(String estabelecimentoEstado) { this.estabelecimentoEstado = estabelecimentoEstado; }
    public UUID getPacienteId() { return pacienteId; }
    public void setPacienteId(UUID pacienteId) { this.pacienteId = pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public void setPacienteNome(String pacienteNome) { this.pacienteNome = pacienteNome; }
    public String getPacienteCpf() { return pacienteCpf; }
    public void setPacienteCpf(String pacienteCpf) { this.pacienteCpf = pacienteCpf; }
    public TipoGuia getTipo() { return tipo; }
    public void setTipo(TipoGuia tipo) { this.tipo = tipo; }
    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }
    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    public LocalDate getDataSolicitada() { return dataSolicitada; }
    public void setDataSolicitada(LocalDate dataSolicitada) { this.dataSolicitada = dataSolicitada; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public PrioridadeGuia getPrioridade() { return prioridade; }
    public void setPrioridade(PrioridadeGuia prioridade) { this.prioridade = prioridade; }
    public StatusGuia getStatus() { return status; }
    public void setStatus(StatusGuia status) { this.status = status; }
    public Long getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(Long solicitanteId) { this.solicitanteId = solicitanteId; }
    public String getSolicitanteNome() { return solicitanteNome; }
    public void setSolicitanteNome(String solicitanteNome) { this.solicitanteNome = solicitanteNome; }
    public LocalDate getDataAgendamento() { return dataAgendamento; }
    public void setDataAgendamento(LocalDate dataAgendamento) { this.dataAgendamento = dataAgendamento; }
    public LocalTime getHoraAgendamento() { return horaAgendamento; }
    public void setHoraAgendamento(LocalTime horaAgendamento) { this.horaAgendamento = horaAgendamento; }
    public Long getAgendadoPorId() { return agendadoPorId; }
    public void setAgendadoPorId(Long agendadoPorId) { this.agendadoPorId = agendadoPorId; }
    public String getAgendadoPorNome() { return agendadoPorNome; }
    public void setAgendadoPorNome(String agendadoPorNome) { this.agendadoPorNome = agendadoPorNome; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public LocalDateTime getLembretePendenteEnviadoEm() { return lembretePendenteEnviadoEm; }
    public void setLembretePendenteEnviadoEm(LocalDateTime lembretePendenteEnviadoEm) {
        this.lembretePendenteEnviadoEm = lembretePendenteEnviadoEm;
    }
    public LocalDateTime getNotificacaoAgendamentoEnviadaEm() { return notificacaoAgendamentoEnviadaEm; }
    public void setNotificacaoAgendamentoEnviadaEm(LocalDateTime notificacaoAgendamentoEnviadaEm) {
        this.notificacaoAgendamentoEnviadaEm = notificacaoAgendamentoEnviadaEm;
    }
    public LocalDateTime getLembretePendenteTentativaEm() { return lembretePendenteTentativaEm; }
    public void setLembretePendenteTentativaEm(LocalDateTime value) { this.lembretePendenteTentativaEm = value; }
    public int getLembretePendenteTentativas() { return lembretePendenteTentativas; }
    public void incrementarLembretePendenteTentativas() { this.lembretePendenteTentativas++; }
    public void limparTentativasLembretePendente() {
        this.lembretePendenteTentativaEm = null;
        this.lembretePendenteTentativas = 0;
    }
    public LocalDateTime getNotificacaoAgendamentoTentativaEm() { return notificacaoAgendamentoTentativaEm; }
    public void setNotificacaoAgendamentoTentativaEm(LocalDateTime value) { this.notificacaoAgendamentoTentativaEm = value; }
    public int getNotificacaoAgendamentoTentativas() { return notificacaoAgendamentoTentativas; }
    public void incrementarNotificacaoAgendamentoTentativas() { this.notificacaoAgendamentoTentativas++; }
    public void limparTentativasNotificacaoAgendamento() {
        this.notificacaoAgendamentoTentativaEm = null;
        this.notificacaoAgendamentoTentativas = 0;
    }
}
