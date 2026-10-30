package com.app.suspiro.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.suspiro.event.RequisicaoStatusAlteradoEvent;

@Component
public class RequisicaoNotificacaoListener {
    private final RequisicaoNotificacaoService notificacaoService;

    public RequisicaoNotificacaoListener(RequisicaoNotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarDepoisDoCommit(RequisicaoStatusAlteradoEvent event) {
        notificacaoService.processarNotificacaoEstado(event.requisicaoId());
    }
}
