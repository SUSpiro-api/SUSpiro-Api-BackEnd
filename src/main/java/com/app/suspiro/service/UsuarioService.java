package com.app.suspiro.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.suspiro.dto.TrocaDeSenhaRequest;
import com.app.suspiro.dto.UsuarioCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.security.SecurityAuth;
import com.app.suspiro.util.GeradorDeSenha;

@Service
public class UsuarioService implements UserDetailsService {

    private static final Map<Perfil, Set<Perfil>> PERFIS_PERMITIDOS = Map.of(
            Perfil.SUPER_ADMIN,
            EnumSet.of(
                    Perfil.GERENTE
            ),
            Perfil.GERENTE,
            EnumSet.of(
                    Perfil.MEDICO,
                    Perfil.ENFERMEIRO,
                    Perfil.ATENDENTE
            )
    );

    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UsuarioService(
            UsuariosRepository usuariosRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String cpf)
            throws UsernameNotFoundException {

        UsuariosModel usuario = usuariosRepository.findByCpf(cpf)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário não encontrado"
                        )
                );

        return new SecurityAuth(usuario);
    }

    public UsuariosModel buscarPorCpf(String cpf) {

        return usuariosRepository.findByCpf(cpf)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário não encontrado"
                        )
                );
    }

    @Transactional
    public UsuariosModel cadastrar(
            UsuarioCadastroRequest request,
            Perfil perfilDoCriador) {

        validarHierarquia(perfilDoCriador, request.getPerfil());

        if (usuariosRepository.existsByCpf(request.getCpf())) {

            throw new RegraDeNegocioException(
                    "Já existe um usuário cadastrado com este CPF"
            );
        }

        String email = normalizarEmail(request.getEmail());

        if (usuariosRepository.existsByEmail(email)) {

            throw new RegraDeNegocioException(
                    "Já existe um usuário cadastrado com este e-mail"
            );
        }

        String senha = GeradorDeSenha.gerar();

        UsuariosModel usuario = new UsuariosModel();

        usuario.setCpf(request.getCpf());
        usuario.setEmail(email);
        usuario.setNome(request.getNome().trim());
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(request.getPerfil());
        usuario.setAtivo(true);
        usuario.setSenhaProvisoria(true);

        UsuariosModel salvo = usuariosRepository.save(usuario);

        emailService.enviarPrimeiroAcesso(salvo, senha);

        return salvo;
    }

    @Transactional(readOnly = true)
    public Page<UsuariosModel> listar(Perfil perfilDoSolicitante, Pageable paginacao) {

        Set<Perfil> visiveis =
                PERFIS_PERMITIDOS.getOrDefault(perfilDoSolicitante, Set.of());

        if (visiveis.isEmpty()) {
            return Page.empty(paginacao);
        }

        return usuariosRepository.findByPerfilIn(visiveis, paginacao);
    }

    @Transactional
    public void trocarSenha(String cpf, TrocaDeSenhaRequest request) {

        UsuariosModel usuario = buscarPorCpf(cpf);

        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) {

            throw new RegraDeNegocioException("A senha atual está incorreta");
        }

        if (passwordEncoder.matches(request.getNovaSenha(), usuario.getSenha())) {

            throw new RegraDeNegocioException(
                    "A nova senha deve ser diferente da senha atual"
            );
        }

        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        usuario.setSenhaProvisoria(false);

        usuariosRepository.save(usuario);
    }

    private void validarHierarquia(Perfil perfilDoCriador, Perfil perfilDesejado) {

        Set<Perfil> permitidos =
                PERFIS_PERMITIDOS.getOrDefault(perfilDoCriador, Set.of());

        if (!permitidos.contains(perfilDesejado)) {

            throw new AccessDeniedException(
                    "O perfil " + perfilDoCriador
                            + " não pode cadastrar usuários com o perfil "
                            + perfilDesejado
            );
        }
    }

    private String normalizarEmail(String email) {

        return email.trim().toLowerCase();
    }
}
