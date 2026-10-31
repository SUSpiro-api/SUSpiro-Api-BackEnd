package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.RequisicoesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.RequisicoesRepository;
import com.app.suspiro.repository.UsuariosRepository;

@Service
public class RequisicaoNotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(RequisicaoNotificacaoService.class);

    private final RequisicoesRepository requisicoesRepository;
    private final UsuariosRepository usuariosRepository;
    private final PacientesRepository pacientesRepository;
    private final EmailService emailService;
    private final Clock clock;

    @Value("${suspiro.guias.notificacoes.retry-minutes:60}")
    private int retryMinutes = 60;

    public RequisicaoNotificacaoService(RequisicoesRepository requisicoesRepository,
            UsuariosRepository usuariosRepository, PacientesRepository pacientesRepository,
            EmailService emailService, Clock clock) {
        this.requisicoesRepository = requisicoesRepository;
        this.usuariosRepository = usuariosRepository;
        this.pacientesRepository = pacientesRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Transactional
    public void processarNotificacaoEstado(Long id) {
        RequisicoesModel requisicao = requisicoesRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Requisição não encontrada"));
        if (requisicao.getNotificacaoStatusEnviado() == requisicao.getStatus()
                || tentativaRecente(requisicao.getNotificacaoTentativaEm())) return;

        String email = buscarEmail(requisicao);
        if (emailService.enviarAcompanhamento(requisicao, email)) {
            requisicao.setNotificacaoStatusEnviado(requisicao.getStatus());
            requisicao.limparTentativasNotificacao();
        } else {
            requisicao.setNotificacaoTentativaEm(LocalDateTime.now(clock));
            requisicao.incrementarNotificacaoTentativas();
            log.warn("Notificação de requisição pendente de entrega (id={}, status={}, tentativas={})",
                    id, requisicao.getStatus(), requisicao.getNotificacaoTentativas());
        }
        requisicoesRepository.save(requisicao);
    }

    @Transactional
    public void processarLembretePendente(Long id) {
        RequisicoesModel requisicao = requisicoesRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Requisição não encontrada"));
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.minusDays(5);
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE
                || requisicao.getCriadoEm() == null || requisicao.getCriadoEm().isAfter(limite)
                || (requisicao.getLembretePendenteEnviadoEm() != null && requisicao.getLembretePendenteEnviadoEm().isAfter(limite))
                || tentativaRecente(requisicao.getLembretePendenteTentativaEm())) return;

        String email = buscarEmail(requisicao);
        if (emailService.enviarLembreteRequisicaoPendente(requisicao, email)) {
            requisicao.setLembretePendenteEnviadoEm(agora);
            requisicao.limparTentativasLembretePendente();
        } else {
            requisicao.setLembretePendenteTentativaEm(agora);
            requisicao.incrementarLembretePendenteTentativas();
            log.warn("Lembrete de requisição pendente de entrega (id={}, tentativas={})", id, requisicao.getLembretePendenteTentativas());
        }
        requisicoesRepository.save(requisicao);
    }

    private boolean tentativaRecente(LocalDateTime tentativa) {
        return tentativa != null && tentativa.isAfter(LocalDateTime.now(clock).minusMinutes(Math.max(1, retryMinutes)));
    }

    private String buscarEmail(RequisicoesModel requisicao) {
        if (requisicao.getPaciente() == null || requisicao.getPaciente().getId() == null) return null;
        Optional<UsuariosModel> conta = usuariosRepository.findByPacienteIdAndPerfil(requisicao.getPaciente().getId(), Perfil.PACIENTE);
        String emailConta = conta.map(UsuariosModel::getEmail).map(String::trim).filter(this::emailPreenchido).orElse(null);
        if (emailConta != null) return emailConta;
        return pacientesRepository.findById(requisicao.getPaciente().getId()).map(PacientesModel::getEmail).map(String::trim)
                .filter(this::emailPreenchido).orElse(null);
    }

    private boolean emailPreenchido(String email) { return email != null && !email.isBlank(); }
}
