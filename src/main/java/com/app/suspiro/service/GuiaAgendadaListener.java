package com.app.suspiro.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.suspiro.event.GuiaAgendadaEvent;

@Component
public class GuiaAgendadaListener {

    private final GuiaNotificacaoService notificacaoService;

    public GuiaAgendadaListener(GuiaNotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarConfirmacaoDepoisDoCommit(GuiaAgendadaEvent event) {
        notificacaoService.enviarConfirmacaoAgendamento(event.guiaId());
    }
}
