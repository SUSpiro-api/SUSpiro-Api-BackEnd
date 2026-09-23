package com.app.suspiro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class RequisicaoService {

    private static final Logger log = LoggerFactory.getLogger(RequisicaoService.class);

    private static final int TENTATIVAS_DE_PROTOCOLO = 5;

    // Médicos e enfermeiros só enxergam as requisições que eles mesmos criaram.
    private static final Set<Perfil> VEEM_APENAS_AS_PROPRIAS =
            EnumSet.of(Perfil.MEDICO, Perfil.ENFERMEIRO);

    private final RequisicoesRepository requisicoesRepository;
    private final AgendamentosRepository agendamentosRepository;
    private final PacientesRepository pacientesRepository;
    private final EmailService emailService;
    private final Clock clock;

    public RequisicaoService(
            RequisicoesRepository requisicoesRepository,
            AgendamentosRepository agendamentosRepository,
            PacientesRepository pacientesRepository,
            EmailService emailService,
            Clock clock) {

        this.requisicoesRepository = requisicoesRepository;
        this.agendamentosRepository = agendamentosRepository;
        this.pacientesRepository = pacientesRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Transactional
    public RequisicoesModel cadastrar(
            RequisicaoCadastroRequest request,
            UsuariosModel solicitante) {

        PacientesModel paciente = pacientesRepository
                .findById(request.getPacienteId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Paciente não encontrado")
                );

        if (request.getDataVencimento().isBefore(hoje())) {

            throw new RegraDeNegocioException(
                    "A data de vencimento não pode estar no passado"
            );
        }

        RequisicoesModel requisicao = new RequisicoesModel();

        requisicao.setProtocolo(gerarProtocoloUnico());
        requisicao.setPaciente(paciente);
        requisicao.setSolicitante(solicitante);
        requisicao.setDescricao(request.getDescricao().trim());
        requisicao.setObservacao(textoOpcional(request.getObservacao()));
        requisicao.setDataVencimento(request.getDataVencimento());
        requisicao.setStatus(StatusRequisicao.PENDENTE);

        RequisicoesModel salva = requisicoesRepository.save(requisicao);

        emailService.enviarAcompanhamento(salva);

        return salva;
    }

    @Transactional(readOnly = true)
    public Page<RequisicoesModel> listar(
            StatusRequisicao status,
            UUID pacienteId,
            UsuariosModel solicitante,
            Pageable paginacao) {

        Long somenteDoSolicitante =
                VEEM_APENAS_AS_PROPRIAS.contains(solicitante.getPerfil())
                        ? solicitante.getId()
                        : null;

        return requisicoesRepository.buscar(
                status, pacienteId, somenteDoSolicitante, paginacao
        );
    }

    @Transactional(readOnly = true)
    public RequisicoesModel buscarPorId(Long id, UsuariosModel usuario) {

        RequisicoesModel requisicao = buscar(id);

        validarVisibilidade(requisicao, usuario);

        return requisicao;
    }

    @Transactional
    public RequisicoesModel agendar(
            Long id,
            AgendamentoRequest request,
            UsuariosModel atendente) {

        RequisicoesModel requisicao = buscar(id);

        if (requisicao.getStatus() != StatusRequisicao.PENDENTE) {

            throw new RegraDeNegocioException(
                    "Apenas requisições pendentes podem ser agendadas. "
                            + "Situação atual: " + requisicao.getStatus()
            );
        }

        if (estaVencida(requisicao)) {

            throw new RegraDeNegocioException(
                    "A requisição venceu em " + requisicao.getDataVencimento()
                            + " e não pode mais ser agendada"
            );
        }

        if (agendamentosRepository.existsByRequisicaoId(requisicao.getId())) {

            throw new RegraDeNegocioException(
                    "Esta requisição já possui um agendamento"
            );
        }

        AgendamentosModel agendamento = new AgendamentosModel();

        agendamento.setRequisicao(requisicao);
        agendamento.setPaciente(requisicao.getPaciente());
        agendamento.setAgendadoPor(atendente);
        agendamento.setDataHora(request.getDataHora());
        agendamento.setLocal(request.getLocal().trim());
        agendamento.setObservacao(textoOpcional(request.getObservacao()));

        agendamentosRepository.save(agendamento);

        requisicao.setAgendamento(agendamento);
        requisicao.setStatus(StatusRequisicao.AGENDADA);

        RequisicoesModel salva = requisicoesRepository.save(requisicao);

        emailService.enviarAcompanhamento(salva);

        return salva;
    }

    @Transactional
    public RequisicoesModel concluir(Long id) {

        RequisicoesModel requisicao = buscar(id);

        if (requisicao.getStatus() != StatusRequisicao.AGENDADA) {

            throw new RegraDeNegocioException(
                    "Apenas requisições agendadas podem ser concluídas. "
                            + "Situação atual: " + requisicao.getStatus()
            );
        }

        requisicao.setStatus(StatusRequisicao.CONCLUIDA);

        RequisicoesModel salva = requisicoesRepository.save(requisicao);

        emailService.enviarAcompanhamento(salva);

        return salva;
    }

    // Consulta pública do paciente. Exige o CPF junto com o protocolo para
    // que um número adivinhado não revele dados de saúde de outra pessoa.
    @Transactional(readOnly = true)
    public RequisicoesModel acompanhar(String protocolo, String cpf) {

        return requisicoesRepository
                .findByProtocoloAndPacienteCpf(protocolo, cpf)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Nenhuma requisição encontrada para este protocolo e CPF"
                        )
                );
    }

    // Marca como VENCIDA toda requisição ainda PENDENTE cuja data de
    // vencimento já passou. Requisições agendadas não vencem.
    @Transactional
    public int vencerRequisicoesAtrasadas() {

        List<RequisicoesModel> atrasadas =
                requisicoesRepository.findByStatusAndDataVencimentoBefore(
                        StatusRequisicao.PENDENTE, hoje()
                );

        for (RequisicoesModel requisicao : atrasadas) {

            requisicao.setStatus(StatusRequisicao.VENCIDA);

            requisicoesRepository.save(requisicao);

            emailService.enviarAcompanhamento(requisicao);
        }

        if (!atrasadas.isEmpty()) {
            log.info("{} requisição(ões) marcada(s) como vencida(s)", atrasadas.size());
        }

        return atrasadas.size();
    }

    private RequisicoesModel buscar(Long id) {

        return requisicoesRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Requisição não encontrada")
                );
    }

    private void validarVisibilidade(RequisicoesModel requisicao, UsuariosModel usuario) {

        if (VEEM_APENAS_AS_PROPRIAS.contains(usuario.getPerfil())
                && !requisicao.getSolicitante().getId().equals(usuario.getId())) {

            throw new AccessDeniedException(
                    "O usuário só pode visualizar as requisições que cadastrou"
            );
        }
    }

    private boolean estaVencida(RequisicoesModel requisicao) {

        return requisicao.getDataVencimento().isBefore(hoje());
    }

    private LocalDate hoje() {

        return LocalDate.now(clock);
    }

    private String gerarProtocoloUnico() {

        for (int i = 0; i < TENTATIVAS_DE_PROTOCOLO; i++) {

            String protocolo = GeradorDeProtocolo.gerar();

            if (!requisicoesRepository.existsByProtocolo(protocolo)) {
                return protocolo;
            }
        }

        throw new IllegalStateException(
                "Não foi possível gerar um protocolo único para a requisição"
        );
    }

    private String textoOpcional(String texto) {

        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
