package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.GuiaRepository;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.UsuariosRepository;

@Service
public class GuiaNotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(GuiaNotificacaoService.class);

    private final GuiaRepository guiaRepository;
    private final UsuariosRepository usuariosRepository;
    private final PacientesRepository pacientesRepository;
    private final EmailService emailService;
    private final Clock clock;

    @Value("${suspiro.guias.notificacoes.retry-minutes:60}")
    private int intervaloRetentativaMinutos = 60;

    public GuiaNotificacaoService(
            GuiaRepository guiaRepository,
            UsuariosRepository usuariosRepository,
            PacientesRepository pacientesRepository,
            EmailService emailService,
            Clock clock) {
        this.guiaRepository = guiaRepository;
        this.usuariosRepository = usuariosRepository;
        this.pacientesRepository = pacientesRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    /** Chamado imediatamente quando a recepção agenda a guia. Falhas de e-mail são repetidas pelo job. */
    @Transactional
    public void enviarConfirmacaoAgendamento(Long guiaId) {
        GuiaModel guia = guiaRepository.findByIdForUpdate(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia não encontrada"));

        if (guia.getStatus() != StatusGuia.AGENDADO || guia.getNotificacaoAgendamentoEnviadaEm() != null) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        if (tentativaRecente(guia.getNotificacaoAgendamentoTentativaEm(), agora)) {
            return;
        }

        String email = buscarEmailDoPaciente(guia);
        if (emailService.enviarConfirmacaoAgendamentoGuia(guia, email)) {
            guia.setNotificacaoAgendamentoEnviadaEm(agora);
            guia.limparTentativasNotificacaoAgendamento();
        } else {
            guia.setNotificacaoAgendamentoTentativaEm(agora);
            guia.incrementarNotificacaoAgendamentoTentativas();
            log.warn("Falha de entrega da confirmação da guia {} (tentativa {}).",
                    guia.getId(), guia.getNotificacaoAgendamentoTentativas());
        }
        guiaRepository.save(guia);
    }

    /** Processa um lembrete por transação, bloqueando a guia para evitar envios simultâneos duplicados. */
    @Transactional
    public void processarLembrete(Long guiaId) {
        GuiaModel guia = guiaRepository.findByIdForUpdate(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia não encontrada"));

        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.minusDays(5);
        if (guia.getStatus() != StatusGuia.AGUARDANDO_AGENDAMENTO
                || guia.getCriadoEm() == null
                || guia.getCriadoEm().isAfter(limite)
                || (guia.getLembretePendenteEnviadoEm() != null
                    && guia.getLembretePendenteEnviadoEm().isAfter(limite))
                || tentativaRecente(guia.getLembretePendenteTentativaEm(), agora)) {
            return;
        }

        String email = buscarEmailDoPaciente(guia);
        if (emailService.enviarLembreteGuiaNaoAgendada(guia, email)) {
            guia.setLembretePendenteEnviadoEm(agora);
            guia.limparTentativasLembretePendente();
        } else {
            guia.setLembretePendenteTentativaEm(agora);
            guia.incrementarLembretePendenteTentativas();
            log.warn("Falha de entrega do lembrete da guia {} (tentativa {}).",
                    guia.getId(), guia.getLembretePendenteTentativas());
        }
        guiaRepository.save(guia);
    }

    /** Reenvia confirmações que falharam por indisponibilidade do SMTP. */
    @Transactional
    public void reprocessarConfirmacaoPendente(Long guiaId) {
        GuiaModel guia = guiaRepository.findByIdForUpdate(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia não encontrada"));

        LocalDateTime agora = LocalDateTime.now(clock);
        if (guia.getStatus() != StatusGuia.AGENDADO
                || guia.getNotificacaoAgendamentoEnviadaEm() != null
                || tentativaRecente(guia.getNotificacaoAgendamentoTentativaEm(), agora)) {
            return;
        }

        String email = buscarEmailDoPaciente(guia);
        if (emailService.enviarConfirmacaoAgendamentoGuia(guia, email)) {
            guia.setNotificacaoAgendamentoEnviadaEm(agora);
            guia.limparTentativasNotificacaoAgendamento();
        } else {
            guia.setNotificacaoAgendamentoTentativaEm(agora);
            guia.incrementarNotificacaoAgendamentoTentativas();
            log.warn("Falha de reenvio da confirmação da guia {} (tentativa {}).",
                    guia.getId(), guia.getNotificacaoAgendamentoTentativas());
        }
        guiaRepository.save(guia);
    }

    private boolean tentativaRecente(LocalDateTime tentativaEm, LocalDateTime agora) {
        int intervalo = Math.max(1, intervaloRetentativaMinutos);
        return tentativaEm != null && tentativaEm.isAfter(agora.minusMinutes(intervalo));
    }

    private String buscarEmailDoPaciente(GuiaModel guia) {
        Optional<UsuariosModel> conta = usuariosRepository
                .findByPacienteIdAndPerfil(guia.getPacienteId(), Perfil.PACIENTE);
        String emailDaConta = conta.map(UsuariosModel::getEmail).map(String::trim).filter(this::emailPreenchido).orElse(null);
        if (emailDaConta != null) {
            return emailDaConta;
        }

        // Fallback para cadastros legados: prioriza sempre o e-mail da conta quando disponível.
        return pacientesRepository.findById(guia.getPacienteId())
                .map(PacientesModel::getEmail).map(String::trim)
                .filter(this::emailPreenchido)
                .orElseGet(() -> {
                    log.warn("Nenhum e-mail cadastrado para o paciente da guia {}", guia.getId());
                    return null;
                });
    }

    private boolean emailPreenchido(String email) {
        return email != null && !email.isBlank();
    }
}
