package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.repository.GuiaRepository;

@Component
public class GuiaNotificacaoJob {

    private static final Logger log = LoggerFactory.getLogger(GuiaNotificacaoJob.class);

    private final GuiaRepository guiaRepository;
    private final GuiaNotificacaoService notificacaoService;
    private final Clock clock;

    @Value("${suspiro.guias.notificacoes.batch-size:100}")
    private int batchSize = 100;

    @Value("${suspiro.guias.notificacoes.max-items-per-cycle:1000}")
    private int maxItemsPerCycle = 1000;

    @Value("${suspiro.guias.notificacoes.retry-minutes:60}")
    private int retryMinutes = 60;

    public GuiaNotificacaoJob(
            GuiaRepository guiaRepository,
            GuiaNotificacaoService notificacaoService,
            Clock clock) {
        this.guiaRepository = guiaRepository;
        this.notificacaoService = notificacaoService;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${GUIAS_NOTIFICACAO_INTERVALO_MS:3600000}",
            initialDelayString = "${GUIAS_NOTIFICACAO_ATRASO_INICIAL_MS:60000}")
    public void verificarNotificacoes() {
        executar();
    }

    private void executar() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limiteLembrete = agora.minusDays(5);
        LocalDateTime tentativaApos = agora.minusMinutes(Math.max(1, retryMinutes));
        int lote = Math.max(1, Math.min(batchSize, 500));
        int maximo = Math.max(lote, Math.min(maxItemsPerCycle, 10000));

        processarLotesLembretes(limiteLembrete, tentativaApos, lote, maximo);
        processarLotesConfirmacoes(tentativaApos, lote, maximo);
    }

    private void processarLotesLembretes(
            LocalDateTime limite, LocalDateTime tentativaApos, int lote, int maximo) {
        int processadas = 0;
        List<Long> ignoradas = new ArrayList<>(List.of(-1L));
        while (processadas < maximo) {
            List<Long> ids = guiaRepository.buscarIdsDeGuiasQuePrecisamDeLembrete(
                    StatusGuia.AGUARDANDO_AGENDAMENTO, limite, tentativaApos, ignoradas,
                    PageRequest.of(0, Math.min(lote, maximo - processadas)));
            if (ids.isEmpty()) return;
            ignoradas.addAll(ids);
            for (Long guiaId : ids) {
                try {
                    notificacaoService.processarLembrete(guiaId);
                } catch (RuntimeException e) {
                    log.error("Falha ao processar lembrete da guia {}", guiaId, e);
                }
                processadas++;
                if (processadas >= maximo) break;
            }
        }
        log.warn("Limite de {} lembretes por ciclo atingido; itens restantes serão processados no próximo ciclo.", maximo);
    }

    private void processarLotesConfirmacoes(LocalDateTime tentativaApos, int lote, int maximo) {
        int processadas = 0;
        List<Long> ignoradas = new ArrayList<>(List.of(-1L));
        while (processadas < maximo) {
            List<Long> ids = guiaRepository.buscarIdsDeAgendamentosSemConfirmacao(
                    StatusGuia.AGENDADO, tentativaApos, ignoradas,
                    PageRequest.of(0, Math.min(lote, maximo - processadas)));
            if (ids.isEmpty()) return;
            ignoradas.addAll(ids);
            for (Long guiaId : ids) {
                try {
                    notificacaoService.reprocessarConfirmacaoPendente(guiaId);
                } catch (RuntimeException e) {
                    log.error("Falha ao reprocessar confirmação de agendamento da guia {}", guiaId, e);
                }
                processadas++;
                if (processadas >= maximo) break;
            }
        }
        log.warn("Limite de {} confirmações por ciclo atingido; itens restantes serão processados no próximo ciclo.", maximo);
    }
}
