package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.app.suspiro.dto.GuiaAgendamentoRequest;
import com.app.suspiro.dto.GuiaCadastroRequest;
import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.event.GuiaAgendadaEvent;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.util.GeradorDeProtocolo;
import com.app.suspiro.repository.GuiaRepository;

@Service
public class GuiaService {

    private final GuiaRepository guiaRepository;
    private final PacienteService pacienteService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public GuiaService(
            GuiaRepository guiaRepository,
            PacienteService pacienteService,
            Clock clock,
            ApplicationEventPublisher eventPublisher) {

        this.guiaRepository = guiaRepository;
        this.pacienteService = pacienteService;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public GuiaModel cadastrar(GuiaCadastroRequest request, UsuariosModel solicitante) {

        PacientesModel paciente = pacienteService.buscarPorId(request.getPacienteId());

        if (request.getDataSolicitada().isBefore(hoje())) {

            throw new RegraDeNegocioException("A data solicitada não pode estar no passado");
        }

        // Nome e CPF vêm do cadastro do paciente; os valores enviados pelo cliente são ignorados.
        GuiaModel guia = new GuiaModel();

        String protocolo;
        do { protocolo = GeradorDeProtocolo.gerarProtocoloGuia(); }
        while (guiaRepository.existsByProtocolo(protocolo));
        guia.setProtocolo(protocolo);
        guia.setPacienteId(paciente.getId());
        guia.setPacienteNome(paciente.getNome());
        guia.setPacienteCpf(paciente.getCpf());
        guia.setTipo(request.getTipo());
        guia.setEspecialidade(textoOpcional(request.getEspecialidade()));
        guia.setDetalhe(textoOpcional(request.getDetalhe()));
        guia.setDataSolicitada(request.getDataSolicitada());
        guia.setObservacao(textoOpcional(request.getObservacao()));
        guia.setPrioridade(request.getPrioridade());
        guia.setStatus(StatusGuia.AGUARDANDO_AGENDAMENTO);
        guia.setSolicitanteId(solicitante.getId());
        guia.setSolicitanteNome(solicitante.getNome());

        return guiaRepository.save(guia);
    }

    @Transactional(readOnly = true)
    public List<GuiaModel> listarTodas() {
        return guiaRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm"));
    }

    @Transactional(readOnly = true)
    public List<GuiaModel> listarDoSolicitante(UsuariosModel solicitante) {

        return guiaRepository.findBySolicitanteIdOrderByCriadoEmDesc(solicitante.getId());
    }

    @Transactional(readOnly = true)
    public List<GuiaModel> listarPendentes() {

        return guiaRepository.findByStatusOrderByPrioridadeDescCriadoEmAsc(
                StatusGuia.AGUARDANDO_AGENDAMENTO
        );
    }

    // Histórico completo do paciente, independentemente de quem criou cada guia.
    // Quem pode consultar é definido por perfil no controller.
    @Transactional(readOnly = true)
    public List<GuiaModel> listarDoPaciente(UUID pacienteId) {

        pacienteService.buscarPorId(pacienteId);

        return guiaRepository.findByPacienteIdOrderByCriadoEmDescIdDesc(pacienteId);
    }

    @Transactional(readOnly = true)
    public GuiaModel buscarPorProtocolo(String protocolo) {
        String codigo = protocolo == null ? "" : protocolo.trim().toUpperCase(java.util.Locale.ROOT);
        return guiaRepository.findByProtocolo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia não encontrada para o protocolo informado."));
    }

    @Transactional
    public GuiaModel agendar(Long id, GuiaAgendamentoRequest request, UsuariosModel agendador) {

        // Bloqueia a linha até o commit para impedir que duas recepções agendem a mesma guia.
        GuiaModel guia = guiaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia não encontrada"));

        if (guia.getStatus() != StatusGuia.AGUARDANDO_AGENDAMENTO) {

            throw new RegraDeNegocioException("A guia selecionada não está aguardando agendamento");
        }

        LocalDateTime dataHoraSolicitada = LocalDateTime.of(request.getDataAgendamento(), request.getHoraAgendamento());
        if (!dataHoraSolicitada.isAfter(LocalDateTime.now(clock))) {
            throw new RegraDeNegocioException("A data e o horário do agendamento devem ser futuros.");
        }

        guia.setEstabelecimentoNome(request.getEstabelecimentoNome().trim());
        guia.setEstabelecimentoEndereco(textoOpcional(request.getEstabelecimentoEndereco()));
        guia.setEstabelecimentoCidade(textoOpcional(request.getEstabelecimentoCidade()));
        String estado = textoOpcional(request.getEstabelecimentoEstado());
        guia.setEstabelecimentoEstado(estado == null ? null : estado.toUpperCase(java.util.Locale.ROOT));
        guia.setStatus(StatusGuia.AGENDADO);
        guia.setDataAgendamento(request.getDataAgendamento());
        guia.setHoraAgendamento(request.getHoraAgendamento());
        guia.setAgendadoPorId(agendador.getId());
        guia.setAgendadoPorNome(agendador.getNome());

        GuiaModel salva = guiaRepository.save(guia);
        // O listener envia a confirmação somente depois que esta transação for confirmada.
        eventPublisher.publishEvent(new GuiaAgendadaEvent(salva.getId()));
        return salva;
    }

    private LocalDate hoje() {

        return LocalDate.now(clock);
    }

    private String textoOpcional(String texto) {

        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
