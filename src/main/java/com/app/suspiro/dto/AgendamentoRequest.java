package com.app.suspiro.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AgendamentoRequest {

    @NotNull(message = "A data e hora do agendamento são obrigatórias")
    @Future(message = "O agendamento deve ser para uma data futura")
    private LocalDateTime dataHora;

    @NotBlank(message = "O local é obrigatório")
    @Size(max = 150, message = "O local deve ter no máximo 150 caracteres")
    private String local;

    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
    private String observacao;

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
}
