package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.suspiro.dto.GuiaAgendamentoRequest;
import com.app.suspiro.dto.GuiaCadastroRequest;
import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.GuiaRepository;

@Service
public class GuiaService {

    private final GuiaRepository guiaRepository;
    private final PacienteService pacienteService;
    private final Clock clock;

    public GuiaService(
            GuiaRepository guiaRepository,
            PacienteService pacienteService,
            Clock clock) {

        this.guiaRepository = guiaRepository;
        this.pacienteService = pacienteService;
        this.clock = clock;
    }

    @Transactional
    public GuiaModel cadastrar(GuiaCadastroRequest request, UsuariosModel solicitante) {

        PacientesModel paciente = pacienteService.buscarPorId(request.getPacienteId());

        if (request.getDataSolicitada().isBefore(hoje())) {

            throw new RegraDeNegocioException("A data solicitada não pode estar no passado");
        }

        // Nome e CPF vêm do cadastro do paciente; os valores enviados pelo cliente são ignorados.
        GuiaModel guia = new GuiaModel();

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

    @Transactional
    public GuiaModel agendar(Long id, GuiaAgendamentoRequest request, UsuariosModel agendador) {

        GuiaModel guia = guiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia não encontrada"));

        if (guia.getStatus() != StatusGuia.AGUARDANDO_AGENDAMENTO) {

            throw new RegraDeNegocioException("A guia selecionada não está aguardando agendamento");
        }

        if (request.getDataAgendamento().isBefore(hoje())) {

            throw new RegraDeNegocioException("A data de agendamento não pode estar no passado");
        }

        guia.setStatus(StatusGuia.AGENDADO);
        guia.setDataAgendamento(request.getDataAgendamento());
        guia.setHoraAgendamento(request.getHoraAgendamento());
        guia.setAgendadoPorId(agendador.getId());
        guia.setAgendadoPorNome(agendador.getNome());

        return guiaRepository.save(guia);
    }

    private LocalDate hoje() {

        return LocalDate.now(clock);
    }

    private String textoOpcional(String texto) {

        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
