package com.app.suspiro.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.app.suspiro.dto.RedefinirSenhaRequest;
import com.app.suspiro.dto.UsuarioCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.DadosInvalidosException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.exception.EnvioDeEmailException;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.security.SecurityAuth;
import com.app.suspiro.util.CpfUtil;

@Service
public class UsuarioService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_RECUPERACAO_MINUTOS = 30;

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

        String cpfNormalizado = CpfUtil.normalizar(cpf);
        UsuariosModel usuario = usuariosRepository.findByCpf(cpfNormalizado)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário não encontrado"
                        )
                );

        return new SecurityAuth(usuario);
    }

    public UsuariosModel buscarPorCpf(String cpf) {

        return usuariosRepository.findByCpf(CpfUtil.normalizar(cpf))
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

        String senhaTemporaria = request.getSenhaTemporaria();
        if (senhaTemporaria == null || senhaTemporaria.length() < 8 || senhaTemporaria.length() > 72
                || !senhaTemporaria.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new DadosInvalidosException(
                    "A senha temporária deve ter entre 8 e 72 caracteres e conter letras e números.");
        }

        String cpf = CpfUtil.normalizar(request.getCpf());
        if (!CpfUtil.valido(cpf)) {
            throw new DadosInvalidosException("CPF inválido. Confira os dígitos informados.");
        }

        if (usuariosRepository.existsByCpf(cpf)) {

            throw new RegraDeNegocioException(
                    "Já existe um usuário cadastrado com este CPF"
            );
        }

        String email = normalizarEmail(request.getEmail());

        if (usuariosRepository.existsByEmailIgnoreCase(email)) {

            throw new RegraDeNegocioException(
                    "Já existe um usuário cadastrado com este e-mail"
            );
        }

        UsuariosModel usuario = new UsuariosModel();

        usuario.setCpf(cpf);
        usuario.setEmail(email);
        usuario.setNome(request.getNome().trim());
        usuario.setSenha(passwordEncoder.encode(senhaTemporaria));
        usuario.setPerfil(request.getPerfil());
        usuario.setAtivo(true);
        usuario.setSenhaProvisoria(true);

        // A senha temporária é definida pelo administrador no cadastro; o hash é persistido
        // e a senhaProvisoria obriga a troca no primeiro acesso. Nunca enviar a senha por e-mail.
        return usuariosRepository.save(usuario);
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
    public UsuariosModel atualizarNome(String cpf, String nome) {
        UsuariosModel usuario = buscarPorCpf(cpf);
        usuario.setNome(nome.trim());
        return usuariosRepository.save(usuario);
    }

    /** Revoga os JWTs já emitidos ao incrementar a versão da sessão. */
    @Transactional
    public void revogarSessoesAtivas(String cpf) {
        UsuariosModel usuario = buscarPorCpf(cpf);
        usuario.incrementarTokenVersion();
        usuariosRepository.save(usuario);
    }

    @Transactional
    public UsuariosModel trocarSenha(String cpf, TrocaDeSenhaRequest request) {

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
        usuario.incrementarTokenVersion();
        limparTokenDeRecuperacao(usuario);

        return usuariosRepository.save(usuario);
    }

    /**
     * Solicita uma recuperação sem revelar se o CPF existe.
     * O token enviado por e-mail é aleatório, armazenado somente como hash,
     * expira em 30 minutos e só pode ser usado uma vez.
     */
    @Transactional
    public void redefinirSenha(String cpf) {
        String cpfNormalizado = CpfUtil.normalizar(cpf);
        UsuariosModel usuario = usuariosRepository.findByCpf(cpfNormalizado).orElse(null);
        if (usuario == null || !usuario.isAtivo()
                || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return;
        }

        String token = gerarTokenDeRecuperacao();
        usuario.setTokenRedefinicaoSenhaHash(hashToken(token));
        usuario.setTokenRedefinicaoSenhaExpiraEm(LocalDateTime.now().plusMinutes(TOKEN_RECUPERACAO_MINUTOS));
        usuariosRepository.save(usuario);

        try {
            emailService.enviarLinkRedefinicao(usuario, token);
        } catch (EnvioDeEmailException ex) {
            // Não mantemos um token válido se a entrega falhou. A resposta HTTP
            // continua genérica para evitar revelar se um CPF está cadastrado.
            limparTokenDeRecuperacao(usuario);
            usuariosRepository.save(usuario);
            log.warn("Não foi possível entregar link de recuperação para o CPF informado");
        }
    }

    @Transactional
    public void confirmarRedefinicaoSenha(RedefinirSenhaRequest request) {
        String hash = hashToken(request.token());
        LocalDateTime agora = LocalDateTime.now();
        UsuariosModel usuario = usuariosRepository
                .findByTokenRedefinicaoSenhaHashAndTokenRedefinicaoSenhaExpiraEmAfter(hash, agora)
                .orElseThrow(() -> new RegraDeNegocioException(
                        "O link de recuperação é inválido ou expirou. Solicite outro."));

        if (!usuario.isAtivo()) {
            limparTokenDeRecuperacao(usuario);
            usuariosRepository.save(usuario);
            throw new RegraDeNegocioException("O link de recuperação é inválido ou expirou. Solicite outro.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setSenhaProvisoria(false);
        usuario.incrementarTokenVersion();
        limparTokenDeRecuperacao(usuario);
        usuariosRepository.save(usuario);
    }

    private String gerarTokenDeRecuperacao() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível neste ambiente", ex);
        }
    }

    private void limparTokenDeRecuperacao(UsuariosModel usuario) {
        usuario.setTokenRedefinicaoSenhaHash(null);
        usuario.setTokenRedefinicaoSenhaExpiraEm(null);
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

        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
