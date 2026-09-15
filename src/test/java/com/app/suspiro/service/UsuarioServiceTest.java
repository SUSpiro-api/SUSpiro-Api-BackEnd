package com.app.suspiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
