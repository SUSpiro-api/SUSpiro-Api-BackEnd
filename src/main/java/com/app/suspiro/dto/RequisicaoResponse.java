package com.app.suspiro.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.model.RequisicoesModel;

public record RequisicaoResponse(
        Long id,
        String protocolo,
        UUID pacienteId,
        String pacienteNome,
        String solicitante,
        String descricao,
        String observacao,
        LocalDate dataVencimento,
        StatusRequisicao status,
        AgendamentoResponse agendamento,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static RequisicaoResponse de(RequisicoesModel requisicao) {

        return new RequisicaoResponse(
                requisicao.getId(),
                requisicao.getProtocolo(),
                requisicao.getPaciente().getId(),
                requisicao.getPaciente().getNome(),
                requisicao.getSolicitante().getNome(),
                requisicao.getDescricao(),
                requisicao.getObservacao(),
                requisicao.getDataVencimento(),
                requisicao.getStatus(),
                AgendamentoResponse.de(requisicao.getAgendamento()),
                requisicao.getCriadoEm(),
                requisicao.getAtualizadoEm()
        );
    }
}
