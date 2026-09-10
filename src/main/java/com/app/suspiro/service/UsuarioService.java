package com.app.suspiro.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.suspiro.dto.UsuarioCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.security.SecurityAuth;

@Service
public class UsuarioService implements UserDetailsService {


    private static final Map<Perfil, Set<Perfil>> PERFIS_PERMITIDOS = Map.of(
            Perfil.SUPER_ADMIN,
            EnumSet.of(
                    Perfil.GERENTE,
                    Perfil.MEDICO,
                    Perfil.ENFERMEIRO,
                    Perfil.ATENDENTE
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

    public UsuarioService(UsuariosRepository usuariosRepository, PasswordEncoder passwordEncoder) {

        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
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

        if (email != null && usuariosRepository.existsByEmail(email)) {

            throw new RegraDeNegocioException(
                    "Já existe um usuário cadastrado com este e-mail"
            );
        }

        UsuariosModel usuario = new UsuariosModel();

        usuario.setCpf(request.getCpf());
        usuario.setEmail(email);
        usuario.setNome(request.getNome().trim());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setPerfil(request.getPerfil());
        usuario.setAtivo(true);

        return usuariosRepository.save(usuario);
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

        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }
}
