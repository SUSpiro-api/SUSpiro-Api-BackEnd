package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.app.suspiro.dto.AgendamentoRequest;
import com.app.suspiro.dto.RequisicaoCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.AgendamentosModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.RequisicoesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.AgendamentosRepository;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.RequisicoesRepository;
import com.app.suspiro.util.GeradorDeProtocolo;

class RequisicaoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 23);

    private static final UUID PACIENTE_ID =
            UUID.fromString("3f2b8c1e-7a4d-4e9b-9c1a-2d5e6f7a8b9c");

    private RequisicoesRepository requisicoesRepository;
    private AgendamentosRepository agendamentosRepository;
    private PacientesRepository pacientesRepository;
    private EmailService emailService;
    private RequisicaoService service;

    private PacientesModel paciente;
    private UsuariosModel medico;
    private UsuariosModel atendente;

    @BeforeEach
    void setUp() {

        requisicoesRepository = Mockito.mock(RequisicoesRepository.class);
        agendamentosRepository = Mockito.mock(AgendamentosRepository.class);
        pacientesRepository = Mockito.mock(PacientesRepository.class);
        emailService = Mockito.mock(EmailService.class);

        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Clock clock = Clock.fixed(
                HOJE.atTime(10, 0).atZone(fuso).toInstant(), fuso
        );

        service = new RequisicaoService(
                requisicoesRepository,
                agendamentosRepository,
                pacientesRepository,
                emailService,
                clock
        );

        when(requisicoesRepository.save(any(RequisicoesModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(agendamentosRepository.save(any(AgendamentosModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paciente = new PacientesModel();
        paciente.setId(PACIENTE_ID);
        paciente.setNome("Maria da Silva");
        paciente.setCpf("98765432100");
        paciente.setEmail("maria@exemplo.com");

        medico = usuario(10L, Perfil.MEDICO);
        atendente = usuario(20L, Perfil.ATENDENTE);

        when(pacientesRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
    }

    private UsuariosModel usuario(Long id, Perfil perfil) {

        UsuariosModel usuario = new UsuariosModel();

        usuario.setId(id);
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);

        return usuario;
    }

    private RequisicaoCadastroRequest cadastro(LocalDate vencimento) {

        RequisicaoCadastroRequest request = new RequisicaoCadastroRequest();

        request.setPacienteId(PACIENTE_ID);
        request.setDescricao("  Consulta com cardiologista  ");
        request.setObservacao("   ");
        request.setDataVencimento(vencimento);

        return request;
    }

    private RequisicoesModel requisicao(StatusRequisicao status, LocalDate vencimento) {

        RequisicoesModel requisicao = new RequisicoesModel();

        requisicao.setId(100L);
        requisicao.setProtocolo("1234567890");
        requisicao.setPaciente(paciente);
        requisicao.setSolicitante(medico);
        requisicao.setDescricao("Consulta com cardiologista");
        requisicao.setDataVencimento(vencimento);
        requisicao.setStatus(status);

        when(requisicoesRepository.findById(100L)).thenReturn(Optional.of(requisicao));

        return requisicao;
    }

    private AgendamentoRequest agendamento() {

        AgendamentoRequest request = new AgendamentoRequest();

        request.setDataHora(LocalDateTime.of(2026, 10, 1, 14, 30));
        request.setLocal("  UBS Centro - sala 3  ");

        return request;
    }

  

    @Test
    void cadastroCriaRequisicaoPendenteComProtocoloEEnviaEmail() {

        when(requisicoesRepository.existsByProtocolo(anyString())).thenReturn(false);

        RequisicoesModel salva = service.cadastrar(cadastro(HOJE.plusDays(30)), medico);

        assertEquals(StatusRequisicao.PENDENTE, salva.getStatus());
        assertSame(paciente, salva.getPaciente());
        assertSame(medico, salva.getSolicitante());
        assertEquals("Consulta com cardiologista", salva.getDescricao());
        assertNull(salva.getObservacao());
        assertTrue(salva.getProtocolo().matches("[1-9]\\d{9}"));

        verify(emailService).enviarAcompanhamento(salva);
    }

    @Test
    void cadastroGeraOutroProtocoloQuandoOSorteadoJaExiste() {

        when(requisicoesRepository.existsByProtocolo(anyString()))
                .thenReturn(true, true, false);

        service.cadastrar(cadastro(HOJE), medico);

        verify(requisicoesRepository, times(3)).existsByProtocolo(anyString());
    }

    @Test
    void cadastroFalhaSeNaoConseguirProtocoloUnico() {

        when(requisicoesRepository.existsByProtocolo(anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.cadastrar(cadastro(HOJE), medico));

        verify(requisicoesRepository, never()).save(any());
    }

    @Test
    void cadastroRecusaPacienteInexistente() {

        RequisicaoCadastroRequest request = cadastro(HOJE);
        request.setPacienteId(UUID.randomUUID());

        when(pacientesRepository.findById(request.getPacienteId()))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.cadastrar(request, medico));

        verify(emailService, never()).enviarAcompanhamento(any());
    }

    @Test
    void cadastroRecusaVencimentoNoPassado() {

        assertThrows(RegraDeNegocioException.class,
                () -> service.cadastrar(cadastro(HOJE.minusDays(1)), medico));

        verify(requisicoesRepository, never()).save(any());
    }



    @Test
    void agendamentoCriaRegistroLigadoARequisicaoEAoPaciente() {

        RequisicoesModel requisicao = requisicao(StatusRequisicao.PENDENTE, HOJE.plusDays(5));

        RequisicoesModel salva = service.agendar(100L, agendamento(), atendente);

        ArgumentCaptor<AgendamentosModel> captor =
                ArgumentCaptor.forClass(AgendamentosModel.class);

        verify(agendamentosRepository).save(captor.capture());

        AgendamentosModel criado = captor.getValue();

        assertSame(requisicao, criado.getRequisicao());
        assertSame(paciente, criado.getPaciente());
        assertSame(atendente, criado.getAgendadoPor());
        assertEquals("UBS Centro - sala 3", criado.getLocal());

        assertEquals(StatusRequisicao.AGENDADA, salva.getStatus());
        assertSame(criado, salva.getAgendamento());

        verify(emailService).enviarAcompanhamento(salva);
    }

    @Test
    void agendamentoNoProprioDiaDoVencimentoEhPermitido() {

        requisicao(StatusRequisicao.PENDENTE, HOJE);

        RequisicoesModel salva = service.agendar(100L, agendamento(), atendente);

        assertEquals(StatusRequisicao.AGENDADA, salva.getStatus());
    }

    @Test
    void agendamentoRecusaRequisicaoComDataDeVencimentoPassada() {


        requisicao(StatusRequisicao.PENDENTE, HOJE.minusDays(1));

        assertThrows(RegraDeNegocioException.class,
                () -> service.agendar(100L, agendamento(), atendente));

        verify(agendamentosRepository, never()).save(any());
        verify(emailService, never()).enviarAcompanhamento(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusRequisicao.class, names = "PENDENTE", mode = EnumSource.Mode.EXCLUDE)
    void agendamentoExigeRequisicaoPendente(StatusRequisicao status) {

        requisicao(status, HOJE.plusDays(5));

        assertThrows(RegraDeNegocioException.class,
                () -> service.agendar(100L, agendamento(), atendente));

        verify(agendamentosRepository, never()).save(any());
    }

    @Test
    void agendamentoRecusaRequisicaoInexistente() {

        when(requisicoesRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.agendar(100L, agendamento(), atendente));
    }

  

    @Test
    void conclusaoMarcaComoConcluidaEEnviaEmail() {

        requisicao(StatusRequisicao.AGENDADA, HOJE.minusDays(10));

        RequisicoesModel salva = service.concluir(100L);

        assertEquals(StatusRequisicao.CONCLUIDA, salva.getStatus());
        verify(emailService).enviarAcompanhamento(salva);
    }

    @ParameterizedTest
    @EnumSource(value = StatusRequisicao.class, names = "AGENDADA", mode = EnumSource.Mode.EXCLUDE)
    void conclusaoExigeRequisicaoAgendada(StatusRequisicao status) {

        requisicao(status, HOJE.plusDays(5));

        assertThrows(RegraDeNegocioException.class, () -> service.concluir(100L));

        verify(emailService, never()).enviarAcompanhamento(any());
    }


    @Test
    void vencimentoMarcaPendentesAtrasadasEAvisaCadaPaciente() {

        RequisicoesModel primeira = new RequisicoesModel();
        primeira.setStatus(StatusRequisicao.PENDENTE);
        primeira.setPaciente(paciente);

        RequisicoesModel segunda = new RequisicoesModel();
        segunda.setStatus(StatusRequisicao.PENDENTE);
        segunda.setPaciente(paciente);

        when(requisicoesRepository.findByStatusAndDataVencimentoBefore(
                StatusRequisicao.PENDENTE, HOJE))
                .thenReturn(List.of(primeira, segunda));

        int vencidas = service.vencerRequisicoesAtrasadas();

        assertEquals(2, vencidas);
        assertEquals(StatusRequisicao.VENCIDA, primeira.getStatus());
        assertEquals(StatusRequisicao.VENCIDA, segunda.getStatus());

        verify(emailService).enviarAcompanhamento(primeira);
        verify(emailService).enviarAcompanhamento(segunda);
    }

    @Test
    void vencimentoConsultaApenasPendentesComDataAnteriorAHoje() {

        when(requisicoesRepository.findByStatusAndDataVencimentoBefore(any(), any()))
                .thenReturn(List.of());

        assertEquals(0, service.vencerRequisicoesAtrasadas());

        verify(requisicoesRepository)
                .findByStatusAndDataVencimentoBefore(StatusRequisicao.PENDENTE, HOJE);
        verify(emailService, never()).enviarAcompanhamento(any());
    }

 

    @Test
    void medicoListaApenasAsPropriasRequisicoes() {

        Pageable paginacao = PageRequest.of(0, 20);

        service.listar(null, null, medico, paginacao);

        verify(requisicoesRepository).buscar(null, null, medico.getId(), paginacao);
    }

    @Test
    void atendenteListaTodasAsRequisicoes() {

        Pageable paginacao = PageRequest.of(0, 20);

        service.listar(StatusRequisicao.PENDENTE, PACIENTE_ID, atendente, paginacao);

        verify(requisicoesRepository)
                .buscar(StatusRequisicao.PENDENTE, PACIENTE_ID, null, paginacao);
    }

    @Test
    void medicoNaoVeRequisicaoDeOutroProfissional() {

        requisicao(StatusRequisicao.PENDENTE, HOJE);

        UsuariosModel outroMedico = usuario(11L, Perfil.MEDICO);

        assertThrows(AccessDeniedException.class,
                () -> service.buscarPorId(100L, outroMedico));
    }

    @Test
    void medicoVeAPropriaRequisicaoEAtendenteVeQualquerUma() {

        RequisicoesModel requisicao = requisicao(StatusRequisicao.PENDENTE, HOJE);

        assertSame(requisicao, service.buscarPorId(100L, medico));
        assertSame(requisicao, service.buscarPorId(100L, atendente));
    }



    @Test
    void acompanhamentoEncontraPorProtocoloECpf() {

        RequisicoesModel requisicao = requisicao(StatusRequisicao.AGENDADA, HOJE);

        when(requisicoesRepository.findByProtocoloAndPacienteCpf("1234567890", "98765432100"))
                .thenReturn(Optional.of(requisicao));

        assertSame(requisicao, service.acompanhar("1234567890", "98765432100"));
    }

    @Test
    void acompanhamentoComCpfErradoNaoEncontraNada() {

        when(requisicoesRepository.findByProtocoloAndPacienteCpf(eq("1234567890"), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.acompanhar("1234567890", "00000000000"));
    }

   

    @Test
    void protocoloTemDezDigitosSemZeroAEsquerda() {

        for (int i = 0; i < 1000; i++) {

            String protocolo = GeradorDeProtocolo.gerar();

            assertNotNull(protocolo);
            assertTrue(protocolo.matches("[1-9]\\d{9}"), protocolo);
        }
    }
}
