package com.app.suspiro.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.app.suspiro.dto.UsuarioLoginRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.security.JwtService;
import com.app.suspiro.service.UsuarioService;

class LoginControllerTest {

    @Test
    void loginEmiteTokenCurtoQuandoSenhaAindaEhProvisoria() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        UsuariosModel usuario = usuario(true);
        prepararLogin(authenticationManager, usuarioService, usuario);
        when(jwtService.gerarTokenDePrimeiroAcesso(usuario)).thenReturn("token-primeiro-acesso");

        LoginController controller = new LoginController(authenticationManager, jwtService, usuarioService);
        ResponseEntity<?> response = controller.login(request());

        LoginController.LoginResponse body = (LoginController.LoginResponse) response.getBody();
        assertEquals("token-primeiro-acesso", body.token());
        assertEquals(true, body.senhaProvisoria());
        verify(jwtService).gerarTokenDePrimeiroAcesso(usuario);
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void loginEmiteTokenNormalDepoisDaTrocaDeSenha() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        UsuariosModel usuario = usuario(false);
        prepararLogin(authenticationManager, usuarioService, usuario);
        when(jwtService.gerarToken(usuario)).thenReturn("token-normal");

        LoginController controller = new LoginController(authenticationManager, jwtService, usuarioService);
        ResponseEntity<?> response = controller.login(request());

        LoginController.LoginResponse body = (LoginController.LoginResponse) response.getBody();
        assertEquals("token-normal", body.token());
        assertEquals(false, body.senhaProvisoria());
        verify(jwtService).gerarToken(usuario);
        verifyNoMoreInteractions(jwtService);
    }

    private static void prepararLogin(
            AuthenticationManager authenticationManager,
            UsuarioService usuarioService,
            UsuariosModel usuario) {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("52998224725", null));
        when(usuarioService.buscarPorCpf("52998224725")).thenReturn(usuario);
    }

    private static UsuariosModel usuario(boolean senhaProvisoria) {
        UsuariosModel usuario = new UsuariosModel();
        usuario.setCpf("52998224725");
        usuario.setNome("Pessoa Sintética");
        usuario.setPerfil(Perfil.PACIENTE);
        usuario.setSenhaProvisoria(senhaProvisoria);
        return usuario;
    }

    private static UsuarioLoginRequest request() {
        UsuarioLoginRequest request = new UsuarioLoginRequest();
        request.setCpf("52998224725");
        request.setSenha("SenhaSegura#2026");
        return request;
    }
}
