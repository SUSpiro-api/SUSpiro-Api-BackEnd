package com.app.suspiro.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.suspiro.dto.TrocaDeSenhaRequest;
import com.app.suspiro.dto.UsuarioCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.EnvioDeEmailException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;

class UsuarioServiceTest {

    private UsuariosRepository repository;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private UsuarioService service;

    @BeforeEach
    void setUp() {

        repository = Mockito.mock(UsuariosRepository.class);
        emailService = Mockito.mock(EmailService.class);
        passwordEncoder = new BCryptPasswordEncoder();

        service = new UsuarioService(repository, passwordEncoder, emailService);

        when(repository.save(any(UsuariosModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UsuarioCadastroRequest request(Perfil perfil) {

        UsuarioCadastroRequest request = new UsuarioCadastroRequest();

        request.setCpf("12345678901");
        request.setNome("Fulano de Tal");
        request.setEmail("Fulano@Exemplo.com");
        request.setPerfil(perfil);

        return request;
    }

    private String senhaEnviadaPorEmail() {

        ArgumentCaptor<String> senha = ArgumentCaptor.forClass(String.class);

        verify(emailService)
                .enviarPrimeiroAcesso(any(UsuariosModel.class), senha.capture());

        return senha.getValue();
    }

    @Test
    void superAdminCadastraGerente() {

        UsuariosModel salvo =
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN);

        assertEquals(Perfil.GERENTE, salvo.getPerfil());
        assertEquals("fulano@exemplo.com", salvo.getEmail());
        verify(repository).save(any(UsuariosModel.class));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"SUPER_ADMIN", "MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void superAdminNaoCadastraOutrosPerfis(Perfil perfil) {

        assertThrows(AccessDeniedException.class, () ->
                service.cadastrar(request(perfil), Perfil.SUPER_ADMIN));

        verify(repository, never()).save(any(UsuariosModel.class));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void gerenteCadastraFuncionarios(Perfil perfil) {

        UsuariosModel salvo =
                service.cadastrar(request(perfil), Perfil.GERENTE);

        assertEquals(perfil, salvo.getPerfil());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"SUPER_ADMIN", "GERENTE"})
    void gerenteNaoCadastraGerenteNemSuperAdmin(Perfil perfil) {

        assertThrows(AccessDeniedException.class, () ->
                service.cadastrar(request(perfil), Perfil.GERENTE));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void demaisPerfisNaoCadastramNinguem(Perfil perfilDoCriador) {

        assertThrows(AccessDeniedException.class, () ->
                service.cadastrar(request(Perfil.ATENDENTE), perfilDoCriador));
    }

    @Test
    void senhaEhGeradaPeloSistemaEArmazenadaCriptografada() {

        UsuariosModel salvo =
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN);

        String senha = senhaEnviadaPorEmail();

        assertTrue(passwordEncoder.matches(senha, salvo.getSenha()));
    }

    @Test
    void emailDePrimeiroAcessoEhEnviadoParaOUsuarioSalvo() {

        UsuariosModel salvo =
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN);

        verify(emailService).enviarPrimeiroAcesso(eq(salvo), anyString());
    }

    @Test
    void falhaNoEnvioDoEmailAbortaOCadastro() {

        Mockito.doThrow(new EnvioDeEmailException("falhou", new RuntimeException()))
                .when(emailService)
                .enviarPrimeiroAcesso(any(UsuariosModel.class), anyString());

        assertThrows(EnvioDeEmailException.class, () ->
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN));
    }

    @Test
    void usuarioNasceComSenhaProvisoria() {

        UsuariosModel salvo =
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN);

        assertTrue(salvo.isSenhaProvisoria());
    }

    @Test
    void cpfDuplicadoEhRejeitado() {

        when(repository.existsByCpf(anyString())).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () ->
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN));
    }

    @Test
    void emailDuplicadoEhRejeitado() {

        when(repository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () ->
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN));
    }



    private UsuariosModel usuarioComSenha(String senha) {

        UsuariosModel usuario = new UsuariosModel();

        usuario.setCpf("12345678901");
        usuario.setNome("Fulano de Tal");
        usuario.setEmail("fulano@exemplo.com");
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(Perfil.GERENTE);
        usuario.setAtivo(true);
        usuario.setSenhaProvisoria(true);

        when(repository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        return usuario;
    }

    private TrocaDeSenhaRequest trocaDeSenha(String atual, String nova) {

        TrocaDeSenhaRequest request = new TrocaDeSenhaRequest();

        request.setSenhaAtual(atual);
        request.setNovaSenha(nova);

        return request;
    }

    @Test
    void trocaDeSenhaGravaONovoHashELimpaAFlagProvisoria() {

        UsuariosModel usuario = usuarioComSenha("SenhaAntiga1!");

        service.trocarSenha(
                "12345678901",
                trocaDeSenha("SenhaAntiga1!", "SenhaNova2@")
        );

        assertTrue(passwordEncoder.matches("SenhaNova2@", usuario.getSenha()));
        assertFalse(usuario.isSenhaProvisoria());
        verify(repository).save(usuario);
    }

    @Test
    void trocaDeSenhaExigeASenhaAtualCorreta() {

        usuarioComSenha("SenhaAntiga1!");

        assertThrows(RegraDeNegocioException.class, () ->
                service.trocarSenha(
                        "12345678901",
                        trocaDeSenha("SenhaErrada1!", "SenhaNova2@")
                ));

        verify(repository, never()).save(any(UsuariosModel.class));
    }

    @Test
    void novaSenhaNaoPodeSerIgualAAtual() {

        usuarioComSenha("SenhaAntiga1!");

        assertThrows(RegraDeNegocioException.class, () ->
                service.trocarSenha(
                        "12345678901",
                        trocaDeSenha("SenhaAntiga1!", "SenhaAntiga1!")
                ));

        verify(repository, never()).save(any(UsuariosModel.class));
    }

    private Collection<Perfil> perfisConsultados(Pageable paginacao) {

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Perfil>> perfis =
                ArgumentCaptor.forClass(Collection.class);

        verify(repository).findByPerfilIn(perfis.capture(), eq(paginacao));

        return perfis.getValue();
    }

    @Test
    void gerenteEnxergaApenasOsFuncionariosQueGerencia() {

        Pageable paginacao = PageRequest.of(0, 20);

        when(repository.findByPerfilIn(any(), eq(paginacao)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar(Perfil.GERENTE, paginacao);

        assertEquals(
                Set.of(Perfil.MEDICO, Perfil.ENFERMEIRO, Perfil.ATENDENTE),
                Set.copyOf(perfisConsultados(paginacao))
        );
    }

    @Test
    void superAdminEnxergaApenasGerentes() {

        Pageable paginacao = PageRequest.of(0, 20);

        when(repository.findByPerfilIn(any(), eq(paginacao)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar(Perfil.SUPER_ADMIN, paginacao);

        assertEquals(
                Set.of(Perfil.GERENTE),
                Set.copyOf(perfisConsultados(paginacao))
        );
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void demaisPerfisNaoEnxergamNinguem(Perfil perfil) {

        Page<UsuariosModel> resultado =
                service.listar(perfil, PageRequest.of(0, 20));

        assertTrue(resultado.isEmpty());
        verify(repository, never()).findByPerfilIn(any(), any());
    }
}
