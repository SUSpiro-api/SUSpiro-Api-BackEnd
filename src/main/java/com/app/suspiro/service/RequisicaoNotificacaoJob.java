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

import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.repository.RequisicoesRepository;

@Component
public class RequisicaoNotificacaoJob {
    private static final Logger log = LoggerFactory.getLogger(RequisicaoNotificacaoJob.class);
    private final RequisicoesRepository repository;
    private final RequisicaoNotificacaoService service;
    private final Clock clock;

    @Value("${suspiro.guias.notificacoes.batch-size:100}") private int batchSize = 100;
    @Value("${suspiro.guias.notificacoes.max-items-per-cycle:1000}") private int maxItems = 1000;
    @Value("${suspiro.guias.notificacoes.retry-minutes:60}") private int retryMinutes = 60;

    public RequisicaoNotificacaoJob(RequisicoesRepository repository, RequisicaoNotificacaoService service, Clock clock) {
        this.repository = repository; this.service = service; this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${REQUISICOES_NOTIFICACAO_INTERVALO_MS:3600000}", initialDelayString = "${REQUISICOES_NOTIFICACAO_ATRASO_INICIAL_MS:75000}")
    public void verificarNotificacoes() {
        LocalDateTime agora = LocalDateTime.now(clock);
        int lote = Math.max(1, Math.min(batchSize, 500));
        int maximo = Math.max(lote, Math.min(maxItems, 10000));
        processarEstados(agora.minusMinutes(Math.max(1, retryMinutes)), lote, maximo);
        processarLembretes(agora.minusDays(5), agora.minusMinutes(Math.max(1, retryMinutes)), lote, maximo);
    }

    private void processarEstados(LocalDateTime retryAfter, int lote, int maximo) {
        List<Long> ignorados = new ArrayList<>(List.of(-1L));
        int processadas = 0;
        List<StatusRequisicao> statuses = List.of(StatusRequisicao.values());
        while (processadas < maximo) {
            List<Long> ids = repository.buscarIdsSemNotificacaoDoStatusAtual(statuses, retryAfter, ignorados,
                    PageRequest.of(0, Math.min(lote, maximo - processadas)));
            if (ids.isEmpty()) return;
            ignorados.addAll(ids);
            for (Long id : ids) {
                try { service.processarNotificacaoEstado(id); }
                catch (RuntimeException ex) { log.error("Falha ao processar notificação de requisição {}", id, ex); }
                processadas++;
                if (processadas >= maximo) break;
            }
        }
        log.warn("Limite de notificações de requisições por ciclo atingido: {}", maximo);
    }

    private void processarLembretes(LocalDateTime limite, LocalDateTime retryAfter, int lote, int maximo) {
        List<Long> ignorados = new ArrayList<>(List.of(-1L));
        int processadas = 0;
        while (processadas < maximo) {
            List<Long> ids = repository.buscarIdsDeRequisicoesQuePrecisamDeLembrete(StatusRequisicao.PENDENTE,
                    limite, retryAfter, ignorados, PageRequest.of(0, Math.min(lote, maximo - processadas)));
            if (ids.isEmpty()) return;
            ignorados.addAll(ids);
            for (Long id : ids) {
                try { service.processarLembretePendente(id); }
                catch (RuntimeException ex) { log.error("Falha ao processar lembrete de requisição {}", id, ex); }
                processadas++;
                if (processadas >= maximo) break;
            }
        }
        log.warn("Limite de lembretes de requisições por ciclo atingido: {}", maximo);
    }
}
