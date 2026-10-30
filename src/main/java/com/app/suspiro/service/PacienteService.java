package com.app.suspiro.service;

import java.util.Locale;
import java.util.UUID;

import com.app.suspiro.dto.PacienteCadastroRequest;
import com.app.suspiro.dto.PacienteAtualizacaoRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.DadosInvalidosException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.util.CpfUtil;
import com.app.suspiro.util.GeradorDeSenha;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Service
public class PacienteService {

    private final PacientesRepository pacientesRepository;
    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PacienteService(
            PacientesRepository pacientesRepository,
            UsuariosRepository usuariosRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.pacientesRepository = pacientesRepository;
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** Cadastra o paciente e cria uma credencial aleatória para o fluxo de primeiro acesso com troca obrigatória de senha. */
    @Transactional
    public PacientesModel cadastrar(PacienteCadastroRequest request) {
        String cpf = CpfUtil.normalizar(request.getCpf());
        if (!CpfUtil.valido(cpf)) {
            throw new DadosInvalidosException("CPF inválido. Confira os dígitos informados.");
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (pacientesRepository.existsByCpf(cpf)) {
            throw new RegraDeNegocioException("CPF já cadastrado para um paciente.");
        }
        if (usuariosRepository.existsByCpf(cpf)) {
            throw new RegraDeNegocioException("Já existe uma conta com este CPF.");
        }
        if (usuariosRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("Já existe uma conta cadastrada com este e-mail.");
        }

        PacientesModel paciente = new PacientesModel();
        paciente.setNome(request.getNome().trim());
        paciente.setCpf(cpf);
        paciente.setDataNascimento(request.getDataNascimento());
        paciente.setSexo(textoOpcional(request.getSexo()));
        paciente.setConvenio(textoOpcional(request.getConvenio()));
        paciente.setNumeroCarteirinha(textoOpcional(request.getNumeroCarteirinha()));
        paciente.setTelefone(textoOpcional(request.getTelefone()));
        paciente.setEmail(email);
        paciente.setCep(request.getCep());
        paciente.setLogradouro(request.getLogradouro().trim());
        paciente.setNumero(request.getNumero().trim());
        paciente.setComplemento(textoOpcional(request.getComplemento()));
        paciente.setBairro(request.getBairro().trim());
        paciente.setCidade(request.getCidade().trim());
        paciente.setEstado(request.getEstado().trim().toUpperCase(Locale.ROOT));
        PacientesModel salvo = pacientesRepository.save(paciente);

        // A senha inicial é aleatória, enviada somente ao e-mail cadastrado e persistida apenas como hash.
        // A flag senhaProvisoria bloqueia as rotas de negócio até a troca obrigatória.
        UsuariosModel conta = new UsuariosModel();
        conta.setCpf(cpf);
        conta.setPacienteId(salvo.getId());
        conta.setEmail(email);
        conta.setNome(salvo.getNome());
        conta.setSexo(salvo.getSexo());
        String senhaTemporaria = GeradorDeSenha.gerar();
        conta.setSenha(passwordEncoder.encode(senhaTemporaria));
        conta.setPerfil(Perfil.PACIENTE);
        conta.setAtivo(true);
        conta.setSenhaProvisoria(true);
        UsuariosModel contaSalva = usuariosRepository.save(conta);

        // Se o e-mail falhar, o cadastro é revertido e a equipe recebe um erro explícito.
        // A senha em texto puro existe somente durante esta chamada e nunca é persistida ou registrada.
        emailService.enviarSenhaTemporariaPaciente(contaSalva, senhaTemporaria);

        return salvo;
    }

    @Transactional(readOnly = true)
    public PacientesModel buscarPorId(UUID id) {
        return pacientesRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente não encontrado."));
    }

    @Transactional(readOnly = true)
    public PacientesModel buscarPorCpf(String cpf) {
        return pacientesRepository.findByCpf(CpfUtil.normalizar(cpf))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<PacientesModel> buscarPorTexto(String termo) {
        String valor = termo == null ? "" : termo.trim();
        String cpf = valor.replaceAll("\\D", "");
        return pacientesRepository.buscarPorNomeOuCpf(valor, cpf, PageRequest.of(0, 20));
    }


    /** Perfil próprio: a identidade do paciente é derivada do JWT, nunca de um ID enviado pelo cliente. */
    @Transactional
    public PacientesModel atualizar(UUID pacienteId, PacienteAtualizacaoRequest request) {
        PacientesModel paciente = buscarPorId(pacienteId);
        UsuariosModel conta = usuariosRepository.findByPacienteIdAndPerfil(pacienteId, Perfil.PACIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta vinculada ao paciente não encontrada."));
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        usuariosRepository.findByEmailIgnoreCase(email).ifPresent(existente -> {
            if (!existente.getId().equals(conta.getId())) {
                throw new RegraDeNegocioException("Já existe uma conta cadastrada com este e-mail.");
            }
        });
        aplicarDadosEditaveis(paciente, request, email);
        conta.setNome(paciente.getNome());
        conta.setSexo(paciente.getSexo());
        conta.setEmail(email);
        pacientesRepository.save(paciente);
        usuariosRepository.save(conta);
        return paciente;
    }

    /** Permite correção cadastral do CPF apenas por um SUPER_ADMIN; invalida sessões antigas do paciente. */
    @Transactional
    public PacientesModel alterarCpf(UUID pacienteId, String cpfInformado) {
        String cpf = CpfUtil.normalizar(cpfInformado);
        if (!CpfUtil.valido(cpf)) throw new DadosInvalidosException("CPF inválido. Confira os dígitos informados.");
        PacientesModel paciente = buscarPorId(pacienteId);
        if (!cpf.equals(paciente.getCpf()) && (pacientesRepository.existsByCpf(cpf) || usuariosRepository.existsByCpf(cpf))) {
            throw new RegraDeNegocioException("Este CPF já está vinculado a outro cadastro.");
        }
        paciente.setCpf(cpf);
        usuariosRepository.findByPacienteIdAndPerfil(pacienteId, Perfil.PACIENTE).ifPresent(conta -> {
            conta.setCpf(cpf);
            conta.incrementarTokenVersion();
            usuariosRepository.save(conta);
        });
        return pacientesRepository.save(paciente);
    }

    private void aplicarDadosEditaveis(PacientesModel paciente, PacienteAtualizacaoRequest request, String email) {
        paciente.setNome(request.getNome().trim());
        paciente.setDataNascimento(request.getDataNascimento());
        paciente.setSexo(textoOpcional(request.getSexo()));
        paciente.setConvenio(textoOpcional(request.getConvenio()));
        paciente.setNumeroCarteirinha(textoOpcional(request.getNumeroCarteirinha()));
        paciente.setTelefone(textoOpcional(request.getTelefone()));
        paciente.setEmail(email);
        paciente.setCep(request.getCep());
        paciente.setLogradouro(request.getLogradouro().trim());
        paciente.setNumero(request.getNumero().trim());
        paciente.setComplemento(textoOpcional(request.getComplemento()));
        paciente.setBairro(request.getBairro().trim());
        paciente.setCidade(request.getCidade().trim());
        paciente.setEstado(request.getEstado().trim().toUpperCase(Locale.ROOT));
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
