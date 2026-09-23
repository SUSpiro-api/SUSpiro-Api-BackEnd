package com.app.suspiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.suspiro.config.AppConfig;

@Component
public class VencimentoDeRequisicoesJob {

    private static final Logger log = LoggerFactory.getLogger(VencimentoDeRequisicoesJob.class);

    private final RequisicaoService requisicaoService;

    public VencimentoDeRequisicoesJob(RequisicaoService requisicaoService) {
        this.requisicaoService = requisicaoService;
    }


    @Scheduled(
            cron = "${REQUISICOES_VENCIMENTO_CRON:0 5 0 * * *}",
            zone = AppConfig.FUSO_HORARIO
    )
    public void executarDiariamente() {
        executar();
    }


    @EventListener(ApplicationReadyEvent.class)
    public void executarAoIniciar() {
        executar();
    }

    private void executar() {

        try {

            requisicaoService.vencerRequisicoesAtrasadas();

        } catch (RuntimeException e) {

            log.error("Falha ao verificar o vencimento das requisições", e);
        }
    }
}
