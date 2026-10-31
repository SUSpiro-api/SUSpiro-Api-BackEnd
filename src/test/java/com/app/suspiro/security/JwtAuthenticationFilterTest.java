package com.app.suspiro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.service.UsuarioService;

class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token-primeiro-acesso";
    private static final String CPF = "52998224725";

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permiteTrocaDeSenhaQuandoApiUsaCaminhoBase() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        UsuariosModel usuario = new UsuariosModel();
        usuario.setCpf(CPF);
        usuario.setPerfil(Perfil.PACIENTE);
        usuario.setAtivo(true);
        usuario.setSenhaProvisoria(true);

        when(jwtService.tokenValido(TOKEN)).thenReturn(true);
        when(jwtService.tokenDePrimeiroAcesso(TOKEN)).thenReturn(true);
        when(jwtService.extrairCpf(TOKEN)).thenReturn(CPF);
        when(jwtService.tokenVersaoValida(TOKEN, 0)).thenReturn(true);
        when(usuarioService.loadUserByUsername(CPF)).thenReturn(new SecurityAuth(usuario));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, usuarioService);
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/usuarios/me/senha");
        request.setContextPath("/api");
        request.setServletPath("/usuarios/me/senha");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void bloqueiaRotasDeNegocioParaTokenDePrimeiroAcesso() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        when(jwtService.tokenValido(TOKEN)).thenReturn(true);
        when(jwtService.tokenDePrimeiroAcesso(TOKEN)).thenReturn(true);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, usuarioService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/guias");
        request.setServletPath("/guias");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }
}
