package com.app.suspiro.dto;

import java.time.LocalDateTime;

import com.app.suspiro.model.AgendamentosModel;

public record AgendamentoResponse(
        Long id,
        LocalDateTime dataHora,
        String local,
        String observacao,
        String agendadoPor,
        LocalDateTime criadoEm) {

    public static AgendamentoResponse de(AgendamentosModel agendamento) {

        if (agendamento == null) {
            return null;
        }

        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getDataHora(),
                agendamento.getLocal(),
                agendamento.getObservacao(),
                agendamento.getAgendadoPor().getNome(),
                agendamento.getCriadoEm()
        );
    }
}
