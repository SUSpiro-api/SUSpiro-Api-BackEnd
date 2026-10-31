package com.app.suspiro.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.model.AgendamentosModel;
import com.app.suspiro.model.RequisicoesModel;


public record AcompanhamentoResponse(
        String protocolo,
        String descricao,
        StatusRequisicao status,
        LocalDate dataVencimento,
        LocalDateTime dataHoraAgendamento,
        String localAgendamento,
        LocalDateTime atualizadoEm) {

    public static AcompanhamentoResponse de(RequisicoesModel requisicao) {

        AgendamentosModel agendamento = requisicao.getAgendamento();

        return new AcompanhamentoResponse(
                requisicao.getProtocolo(),
                requisicao.getDescricao(),
                requisicao.getStatus(),
                requisicao.getDataVencimento(),
                agendamento != null ? agendamento.getDataHora() : null,
                agendamento != null ? agendamento.getLocal() : null,
                requisicao.getAtualizadoEm()
        );
    }
}
