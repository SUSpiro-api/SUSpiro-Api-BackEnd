package com.app.suspiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.model.AgendamentosModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.RequisicoesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.RequisicoesRepository;
import com.app.suspiro.repository.UsuariosRepository;

class RequisicaoNotificacaoServiceTest {
    private static final Long REQUISICAO_ID = 321L;
    private static final UUID PACIENTE_ID = UUID.fromString("3f2b8c1e-7a4d-4e9b-9c1a-2d5e6f7a8b9c");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 29, 12, 0);

    private RequisicoesRepository requisicoesRepository;
    private UsuariosRepository usuariosRepository;
    private PacientesRepository pacientesRepository;
    private EmailService emailService;
    private RequisicaoNotificacaoService service;
    private RequisicoesModel requisicao;

    @BeforeEach
    void setUp() {
        requisicoesRepository = Mockito.mock(RequisicoesRepository.class);
        usuariosRepository = Mockito.mock(UsuariosRepository.class);
        pacientesRepository = Mockito.mock(PacientesRepository.class);
        emailService = Mockito.mock(EmailService.class);
        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));
        service = new RequisicaoNotificacaoService(requisicoesRepository, usuariosRepository, pacientesRepository, emailService, clock);

        PacientesModel paciente = new PacientesModel();
        paciente.setId(PACIENTE_ID);
        paciente.setNome("Lucas Exemplo");
        paciente.setEmail("cadastro@example.com");
        requisicao = new RequisicoesModel();
        requisicao.setId(REQUISICAO_ID);
        requisicao.setProtocolo("REQ-321");
        requisicao.setPaciente(paciente);
        requisicao.setDescricao("Consulta cardiológica");
        requisicao.setDataVencimento(LocalDate.of(2026, 12, 1));
        requisicao.setStatus(StatusRequisicao.PENDENTE);
        ReflectionTestUtils.setField(requisicao, "criadoEm", AGORA.minusDays(5));

        UsuariosModel conta = new UsuariosModel();
        conta.setPacienteId(PACIENTE_ID);
        conta.setPerfil(Perfil.PACIENTE);
        conta.setEmail("lucas@example.com");
        when(usuariosRepository.findByPacienteIdAndPerfil(PACIENTE_ID, Perfil.PACIENTE)).thenReturn(Optional.of(conta));
    }

    @Test
    void enviaConfirmacaoERegistraEstadoAceitoPeloSmtp() {
        requisicao.setStatus(StatusRequisicao.AGENDADA);
        AgendamentosModel agendamento = new AgendamentosModel();
        agendamento.setDataHora(LocalDateTime.of(2026, 11, 12, 9, 30));
        agendamento.setLocal("Unidade de Saúde Regional");
        requisicao.setAgendamento(agendamento);
        when(requisicoesRepository.findByIdForUpdate(REQUISICAO_ID)).thenReturn(Optional.of(requisicao));
        when(emailService.enviarAcompanhamento(requisicao, "lucas@example.com")).thenReturn(true);

        service.processarNotificacaoEstado(REQUISICAO_ID);

        assertEquals(StatusRequisicao.AGENDADA, requisicao.getNotificacaoStatusEnviado());
        verify(requisicoesRepository).save(requisicao);
        verify(emailService).enviarAcompanhamento(requisicao, "lucas@example.com");
    }

    @Test
    void enviaLembreteDepoisDeCincoDiasERegistraHorario() {
        when(requisicoesRepository.findByIdForUpdate(REQUISICAO_ID)).thenReturn(Optional.of(requisicao));
        when(emailService.enviarLembreteRequisicaoPendente(requisicao, "lucas@example.com")).thenReturn(true);

        service.processarLembretePendente(REQUISICAO_ID);

        assertEquals(AGORA, requisicao.getLembretePendenteEnviadoEm());
        verify(emailService).enviarLembreteRequisicaoPendente(requisicao, "lucas@example.com");
        verify(requisicoesRepository).save(requisicao);
    }

    @Test
    void naoEnviaLembreteAntesDosCincoDias() {
        ReflectionTestUtils.setField(requisicao, "criadoEm", AGORA.minusDays(4));
        when(requisicoesRepository.findByIdForUpdate(REQUISICAO_ID)).thenReturn(Optional.of(requisicao));

        service.processarLembretePendente(REQUISICAO_ID);

        verify(emailService, never()).enviarLembreteRequisicaoPendente(eq(requisicao), eq("lucas@example.com"));
        verify(requisicoesRepository, never()).save(requisicao);
    }


}
