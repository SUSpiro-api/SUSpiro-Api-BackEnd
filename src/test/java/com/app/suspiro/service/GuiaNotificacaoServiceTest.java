package com.app.suspiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.enums.PrioridadeGuia;
import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.enums.TipoGuia;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.GuiaRepository;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.UsuariosRepository;

class GuiaNotificacaoServiceTest {

    private static final UUID PACIENTE_ID = UUID.fromString("3f2b8c1e-7a4d-4e9b-9c1a-2d5e6f7a8b9c");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 27, 10, 0);

    private GuiaRepository guiaRepository;
    private UsuariosRepository usuariosRepository;
    private PacientesRepository pacientesRepository;
    private EmailService emailService;
    private GuiaNotificacaoService service;
    private GuiaModel guia;
    private UsuariosModel contaPaciente;

    @BeforeEach
    void setUp() {
        guiaRepository = Mockito.mock(GuiaRepository.class);
        usuariosRepository = Mockito.mock(UsuariosRepository.class);
        pacientesRepository = Mockito.mock(PacientesRepository.class);
        emailService = Mockito.mock(EmailService.class);
        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));
        service = new GuiaNotificacaoService(guiaRepository, usuariosRepository, pacientesRepository, emailService, clock);

        guia = new GuiaModel();
        guia.setPacienteId(PACIENTE_ID);
        guia.setPacienteNome("Maria Exemplo");
        guia.setPacienteCpf("11144477735");
        guia.setTipo(TipoGuia.CONSULTA);
        guia.setEspecialidade("Cardiologia");
        guia.setDataSolicitada(LocalDate.of(2026, 11, 5));
        guia.setPrioridade(PrioridadeGuia.NORMAL);
        guia.setStatus(StatusGuia.AGUARDANDO_AGENDAMENTO);
        guia.setSolicitanteId(7L);
        guia.setSolicitanteNome("Dra. Ana");
        guia.setCriadoEm(AGORA.minusDays(5));

        contaPaciente = new UsuariosModel();
        contaPaciente.setPacienteId(PACIENTE_ID);
        contaPaciente.setPerfil(Perfil.PACIENTE);
        contaPaciente.setEmail("maria@example.com");
        when(usuariosRepository.findByPacienteIdAndPerfil(PACIENTE_ID, Perfil.PACIENTE))
                .thenReturn(Optional.of(contaPaciente));
    }

    @Test
    void enviaLembreteQuandoGuiaEstaPendenteHaCincoDias() {
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));
        when(emailService.enviarLembreteGuiaNaoAgendada(guia, "maria@example.com")).thenReturn(true);

        service.processarLembrete(10L);

        assertEquals(AGORA, guia.getLembretePendenteEnviadoEm());
        verify(guiaRepository).save(guia);
    }

    @Test
    void registraFalhaEImpedeRepetirEmailEmCadaCicloDoJob() {
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));
        when(emailService.enviarLembreteGuiaNaoAgendada(guia, "maria@example.com")).thenReturn(false);

        service.processarLembrete(10L);
        service.processarLembrete(10L);

        assertEquals(AGORA, guia.getLembretePendenteTentativaEm());
        assertEquals(1, guia.getLembretePendenteTentativas());
        verify(emailService).enviarLembreteGuiaNaoAgendada(guia, "maria@example.com");
        verify(guiaRepository).save(guia);
    }

    @Test
    void naoEnviaLembreteAntesDeCompletarCincoDias() {
        guia.setCriadoEm(AGORA.minusDays(4));
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));

        service.processarLembrete(10L);

        verify(emailService, never()).enviarLembreteGuiaNaoAgendada(any(), any());
        verify(guiaRepository, never()).save(guia);
    }


    @Test
    void naoRepeteLembreteAntesDePassaremMaisCincoDiasDoUltimoEnvio() {
        guia.setCriadoEm(AGORA.minusDays(20));
        guia.setLembretePendenteEnviadoEm(AGORA.minusDays(4));
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));

        service.processarLembrete(10L);

        verify(emailService, never()).enviarLembreteGuiaNaoAgendada(any(), any());
        verify(guiaRepository, never()).save(guia);
    }

    @Test
    void naoEnviaLembreteQuandoGuiaJaFoiAgendada() {
        guia.setStatus(StatusGuia.AGENDADO);
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));

        service.processarLembrete(10L);

        verify(emailService, never()).enviarLembreteGuiaNaoAgendada(any(), any());
    }

    @Test
    void confirmaAgendamentoERegistraQuandoEmailFoiEnviado() {
        guia.setStatus(StatusGuia.AGENDADO);
        guia.setDataAgendamento(LocalDate.of(2026, 11, 5));
        guia.setHoraAgendamento(LocalTime.of(9, 30));
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));
        when(emailService.enviarConfirmacaoAgendamentoGuia(guia, "maria@example.com")).thenReturn(true);

        service.enviarConfirmacaoAgendamento(10L);

        assertNotNull(guia.getNotificacaoAgendamentoEnviadaEm());
        assertEquals(AGORA, guia.getNotificacaoAgendamentoEnviadaEm());
        verify(guiaRepository).save(guia);
    }

    @Test
    void registraFalhaNaConfirmacaoEPermiteRetryControlado() {
        guia.setStatus(StatusGuia.AGENDADO);
        guia.setDataAgendamento(LocalDate.of(2026, 11, 5));
        guia.setHoraAgendamento(LocalTime.of(9, 30));
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));
        when(emailService.enviarConfirmacaoAgendamentoGuia(guia, "maria@example.com")).thenReturn(false);

        service.enviarConfirmacaoAgendamento(10L);

        assertEquals(AGORA, guia.getNotificacaoAgendamentoTentativaEm());
        assertEquals(1, guia.getNotificacaoAgendamentoTentativas());
        verify(guiaRepository).save(guia);
    }

    @Test
    void usaEmailDoCadastroDoPacienteQuandoContaNaoTemEmail() {
        contaPaciente.setEmail(" ");
        PacientesModel paciente = new PacientesModel();
        paciente.setEmail("contato@example.com");
        when(pacientesRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
        when(guiaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(guia));
        when(emailService.enviarLembreteGuiaNaoAgendada(eq(guia), eq("contato@example.com"))).thenReturn(true);

        service.processarLembrete(10L);

        verify(emailService).enviarLembreteGuiaNaoAgendada(guia, "contato@example.com");
    }
}
