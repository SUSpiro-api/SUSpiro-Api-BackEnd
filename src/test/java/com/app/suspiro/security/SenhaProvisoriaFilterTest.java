package com.app.suspiro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import tools.jackson.databind.ObjectMapper;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;

class SenhaProvisoriaFilterTest {

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permiteTrocaDeSenhaComCaminhoBaseDaAplicacao() throws Exception {
        UsuariosModel usuario = new UsuariosModel();
        usuario.setCpf("52998224725");
        usuario.setPerfil(Perfil.PACIENTE);
        usuario.setSenhaProvisoria(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new SecurityAuth(usuario), null, new SecurityAuth(usuario).getAuthorities()));

        SenhaProvisoriaFilter filter = new SenhaProvisoriaFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/usuarios/me/senha");
        request.setContextPath("/api");
        request.setServletPath("/usuarios/me/senha");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void bloqueiaRotasDeNegocioEnquantoSenhaForProvisoria() throws Exception {
        UsuariosModel usuario = new UsuariosModel();
        usuario.setCpf("52998224725");
        usuario.setPerfil(Perfil.PACIENTE);
        usuario.setSenhaProvisoria(true);
        SecurityAuth principal = new SecurityAuth(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        SenhaProvisoriaFilter filter = new SenhaProvisoriaFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/guias");
        request.setServletPath("/guias");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }
}
