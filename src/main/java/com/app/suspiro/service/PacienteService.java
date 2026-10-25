package com.app.suspiro.service;

import java.util.Locale;
import java.util.UUID;
import java.time.format.DateTimeFormatter;

import com.app.suspiro.dto.PacienteCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.DadosInvalidosException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.util.CpfUtil;
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

    public PacienteService(
            PacientesRepository pacientesRepository,
            UsuariosRepository usuariosRepository,
            PasswordEncoder passwordEncoder) {
        this.pacientesRepository = pacientesRepository;
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Cadastra o paciente e a conta vinculada usando a data de nascimento como senha inicial temporária. */
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
        paciente.setConvenio(textoOpcional(request.getConvenio()));
        paciente.setTelefone(textoOpcional(request.getTelefone()));
        paciente.setEmail(email);
        PacientesModel salvo = pacientesRepository.save(paciente);

        // A data de nascimento é somente uma senha inicial temporária.
        // Formato combinado com a tela: DDMMAAAA, sem barras ou hífens.
        String senhaInicial = salvo.getDataNascimento().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        UsuariosModel conta = new UsuariosModel();
        conta.setCpf(cpf);
        conta.setPacienteId(salvo.getId());
        conta.setEmail(email);
        conta.setNome(salvo.getNome());
        conta.setSenha(passwordEncoder.encode(senhaInicial));
        conta.setPerfil(Perfil.PACIENTE);
        conta.setAtivo(true);
        conta.setSenhaProvisoria(true);
        usuariosRepository.save(conta);

        // O filtro da API bloqueia as demais rotas até que a senha inicial seja trocada.
        // O e-mail é mantido para recuperação de senha por link, não para enviar a data de nascimento.
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

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
