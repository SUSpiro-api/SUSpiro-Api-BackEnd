package com.app.suspiro.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.app.suspiro.enums.PrioridadeGuia;
import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.enums.TipoGuia;
import com.app.suspiro.model.GuiaModel;

public record GuiaResponse(
        Long id,
        String protocolo,
        UUID pacienteId,
        String pacienteNome,
        String pacienteCpf,
        TipoGuia tipo,
        String especialidade,
        String detalhe,
        LocalDate dataSolicitada,
        String observacao,
        PrioridadeGuia prioridade,
        StatusGuia status,
        Long solicitanteId,
        String solicitanteNome,
        LocalDate dataAgendamento,
        LocalTime horaAgendamento,
        String estabelecimentoNome,
        String estabelecimentoEndereco,
        String estabelecimentoCidade,
        String estabelecimentoEstado,
        Long agendadoPorId,
        String agendadoPorNome,
        LocalDateTime criadoEm) {

    public static GuiaResponse de(GuiaModel guia) {
        return de(guia, true);
    }

    public static GuiaResponse de(GuiaModel guia, boolean incluirCpfCompleto) {
        String cpfResposta = guia.getPacienteCpf();
        if (!incluirCpfCompleto && cpfResposta != null && cpfResposta.length() == 11) {
            cpfResposta = "•••.•••.•••-" + cpfResposta.substring(9);
        }
        return new GuiaResponse(
                guia.getId(),
                guia.getProtocolo(),
                guia.getPacienteId(),
                guia.getPacienteNome(),
                cpfResposta,
                guia.getTipo(),
                guia.getEspecialidade(),
                guia.getDetalhe(),
                guia.getDataSolicitada(),
                guia.getObservacao(),
                guia.getPrioridade(),
                guia.getStatus(),
                guia.getSolicitanteId(),
                guia.getSolicitanteNome(),
                guia.getDataAgendamento(),
                guia.getHoraAgendamento(),
                guia.getEstabelecimentoNome(),
                guia.getEstabelecimentoEndereco(),
                guia.getEstabelecimentoCidade(),
                guia.getEstabelecimentoEstado(),
                guia.getAgendadoPorId(),
                guia.getAgendadoPorNome(),
                guia.getCriadoEm());
    }
}
