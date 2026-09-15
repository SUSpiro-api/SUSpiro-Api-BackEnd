package com.app.suspiro.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SenhaProvisoriaFilter extends OncePerRequestFilter {

    public static final String ROTA_DE_TROCA_DE_SENHA = "/usuarios/me/senha";

    private static final String CODIGO = "SENHA_PROVISORIA";

    private final ObjectMapper objectMapper;

    public SenhaProvisoriaFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!precisaTrocarSenha() || ehRotaLiberada(request)) {

            filterChain.doFilter(request, response);
            return;
        }

        responderAcessoBloqueado(response);
    }

    private boolean precisaTrocarSenha() {

        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null
                || !(autenticacao.getPrincipal() instanceof SecurityAuth autenticado)) {

            return false;
        }

        return autenticado.getUsuario().isSenhaProvisoria();
    }

    private boolean ehRotaLiberada(HttpServletRequest request) {

        return ROTA_DE_TROCA_DE_SENHA.equals(request.getRequestURI());
    }

    private void responderAcessoBloqueado(HttpServletResponse response)
            throws IOException {

        Map<String, Object> corpo = new LinkedHashMap<>();

        corpo.put("status", HttpStatus.FORBIDDEN.value());
        corpo.put("codigo", CODIGO);
        corpo.put("mensagem",
                "Sua senha é provisória. Cadastre uma nova senha em "
                        + ROTA_DE_TROCA_DE_SENHA
                        + " para liberar o acesso ao sistema.");

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), corpo);
    }
}
