package com.app.suspiro.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.app.suspiro.enums.PrioridadeGuia;
import com.app.suspiro.enums.TipoGuia;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GuiaCadastroRequest {

    @NotNull(message = "O paciente é obrigatório")
    @Schema(description = "Id do paciente cadastrado. Nome e CPF da guia são obtidos deste cadastro.")
    private UUID pacienteId;

    // Aceitos apenas por compatibilidade com clientes existentes: o backend
    // ignora estes valores e usa os dados oficiais do paciente.
    @Schema(description = "Ignorado. O nome é obtido do cadastro do paciente.", deprecated = true)
    private String pacienteNome;

    @Schema(description = "Ignorado. O CPF é obtido do cadastro do paciente.", deprecated = true)
    private String pacienteCpf;

    @NotNull(message = "O tipo da guia é obrigatório")
    private TipoGuia tipo;

    @Size(max = 120, message = "A especialidade deve ter no máximo 120 caracteres")
    private String especialidade;

    @Size(max = 255, message = "O detalhe deve ter no máximo 255 caracteres")
    private String detalhe;

    @NotNull(message = "A data solicitada é obrigatória")
    private LocalDate dataSolicitada;

    @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
    private String observacao;

    @NotNull(message = "A prioridade é obrigatória")
    private PrioridadeGuia prioridade;

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
}
