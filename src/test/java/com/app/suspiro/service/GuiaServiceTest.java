package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.suspiro.dto.GuiaCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.enums.PrioridadeGuia;
import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.enums.TipoGuia;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.GuiaRepository;

class GuiaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 23);

    private static final UUID PACIENTE_ID =
            UUID.fromString("3f2b8c1e-7a4d-4e9b-9c1a-2d5e6f7a8b9c");

    private static final UUID INEXISTENTE =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private GuiaRepository guiaRepository;
    private PacienteService pacienteService;
    private GuiaService service;
    private ApplicationEventPublisher eventPublisher;

    private PacientesModel paciente;
    private UsuariosModel medico;

    @BeforeEach
    void setUp() {

        guiaRepository = Mockito.mock(GuiaRepository.class);
        pacienteService = Mockito.mock(PacienteService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        Clock clock = Clock.fixed(
                HOJE.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(),
                ZoneId.of("America/Sao_Paulo"));

        service = new GuiaService(guiaRepository, pacienteService, clock, eventPublisher);

        paciente = new PacientesModel();
        paciente.setId(PACIENTE_ID);
        paciente.setNome("Maria Oficial");
        paciente.setCpf("11144477735");

        medico = new UsuariosModel();
        medico.setId(5L);
        medico.setNome("Dra. Ana");
        medico.setPerfil(Perfil.MEDICO);
    }

    @Test
    void cadastrarUsaNomeECpfDoCadastroDoPacienteEIgnoraOsEnviados() {

        when(pacienteService.buscarPorId(PACIENTE_ID)).thenReturn(paciente);
        when(guiaRepository.save(any(GuiaModel.class))).thenAnswer(i -> i.getArgument(0));

        GuiaCadastroRequest request = request(PACIENTE_ID);
        request.setPacienteNome("Nome Manipulado");
        request.setPacienteCpf("12345678909");

        service.cadastrar(request, medico);

        ArgumentCaptor<GuiaModel> salva = ArgumentCaptor.forClass(GuiaModel.class);
        verify(guiaRepository).save(salva.capture());

        assertEquals(PACIENTE_ID, salva.getValue().getPacienteId());
        assertEquals("Maria Oficial", salva.getValue().getPacienteNome());
        assertEquals("11144477735", salva.getValue().getPacienteCpf());
        assertEquals(StatusGuia.AGUARDANDO_AGENDAMENTO, salva.getValue().getStatus());
        assertEquals(5L, salva.getValue().getSolicitanteId());
    }

    @Test
    void cadastrarComPacienteInexistenteNaoPersisteAGuia() {

        when(pacienteService.buscarPorId(INEXISTENTE))
                .thenThrow(new RecursoNaoEncontradoException("Paciente não encontrado."));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.cadastrar(request(INEXISTENTE), medico));

        verify(guiaRepository, never()).save(any());
    }

    @Test
    void listarDoPacienteInexistenteNaoConsultaGuias() {

        when(pacienteService.buscarPorId(INEXISTENTE))
                .thenThrow(new RecursoNaoEncontradoException("Paciente não encontrado."));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.listarDoPaciente(INEXISTENTE));

        verify(guiaRepository, never()).findByPacienteIdOrderByCriadoEmDescIdDesc(any());
    }

    @Test
    void listarDoPacienteSemGuiasRetornaListaVazia() {

        when(pacienteService.buscarPorId(PACIENTE_ID)).thenReturn(paciente);
        when(guiaRepository.findByPacienteIdOrderByCriadoEmDescIdDesc(PACIENTE_ID)).thenReturn(List.of());

        assertTrue(service.listarDoPaciente(PACIENTE_ID).isEmpty());
    }

    private GuiaCadastroRequest request(UUID pacienteId) {

        GuiaCadastroRequest request = new GuiaCadastroRequest();

        request.setPacienteId(pacienteId);
        request.setTipo(TipoGuia.CONSULTA);
        request.setEspecialidade("Cardiologia");
        request.setDataSolicitada(HOJE.plusDays(7));
        request.setPrioridade(PrioridadeGuia.NORMAL);

        return request;
    }
}
