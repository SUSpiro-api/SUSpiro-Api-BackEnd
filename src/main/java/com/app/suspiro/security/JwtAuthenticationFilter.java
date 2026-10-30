package com.app.suspiro.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.suspiro.service.UsuarioService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioService usuarioService) {

        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    private boolean rotaPermitidaNoPrimeiroAcesso(HttpServletRequest request) {
        String caminho = request.getServletPath();
        String metodo = request.getMethod();
        return ("/usuarios/me/senha".equals(caminho) && "PATCH".equalsIgnoreCase(metodo))
                || ("/auth/logout".equals(caminho) && "POST".equalsIgnoreCase(metodo));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7);


        if (!jwtService.tokenValido(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Um JWT de primeiro acesso não pode ser reutilizado em endpoints de negócio,
        // mesmo que o estado da conta seja alterado por outra operação administrativa.
        if (jwtService.tokenDePrimeiroAcesso(token) && !rotaPermitidaNoPrimeiroAcesso(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":403,\"codigo\":\"PRIMEIRO_ACESSO_RESTRITO\",\"mensagem\":\"Conclua a criação da senha antes de continuar.\"}");
            return;
        }

        String cpf =
                jwtService.extrairCpf(token);


        if (SecurityContextHolder
                .getContext()
                .getAuthentication() != null) {

            filterChain.doFilter(request, response);
            return;
        }

        try {

            UserDetails userDetails =
                    usuarioService.loadUserByUsername(cpf);

            // A conta pode ter sido desativada depois da emissão do JWT.
            // Não autentique um token antigo de uma conta agora inativa.
            if (!userDetails.isEnabled()
                    || !(userDetails instanceof SecurityAuth autenticado)
                    || !jwtService.tokenVersaoValida(token, autenticado.getUsuario().getTokenVersion())) {
                SecurityContextHolder.clearContext();
            } else {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception e) {

            // Token válido, mas usuário não encontrado
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}