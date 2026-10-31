package com.app.suspiro.config;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PublicEndpointRateLimitInterceptor implements HandlerInterceptor {

    private static final long JANELA_MS = Duration.ofMinutes(5).toMillis();
    private static final long RETENCAO_MS = Duration.ofMinutes(10).toMillis();
    private static final int LIMITE_CLIENTES = 5000;
    private final ConcurrentHashMap<String, Janela> tentativas = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String caminho = request.getRequestURI();
        String contexto = request.getContextPath();
        if (contexto != null && !contexto.isBlank() && caminho.startsWith(contexto)) {
            caminho = caminho.substring(contexto.length());
        }
        Integer limite = limitePara(caminho, request.getMethod());
        if (limite == null) return true;

        long agora = System.currentTimeMillis();
        String chave = request.getRemoteAddr() + ":" + caminho;
        AtomicBoolean permitido = new AtomicBoolean(true);

        if (!tentativas.containsKey(chave) && tentativas.size() >= LIMITE_CLIENTES) {
            tentativas.entrySet().removeIf(item -> agora - item.getValue().inicio() >= RETENCAO_MS);
            if (!tentativas.containsKey(chave) && tentativas.size() >= LIMITE_CLIENTES) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Retry-After", "300");
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":429,\"mensagem\":\"Muitas tentativas. Aguarde alguns minutos e tente novamente.\"}");
                return false;
            }
        }

        tentativas.compute(chave, (ip, janela) -> {
            if (janela == null || agora - janela.inicio() >= JANELA_MS) {
                return new Janela(agora, 1);
            }
            int quantidade = janela.quantidade() + 1;
            permitido.set(quantidade <= limite);
            return new Janela(janela.inicio(), quantidade);
        });

        if (tentativas.size() > LIMITE_CLIENTES) {
            tentativas.entrySet().removeIf(item -> agora - item.getValue().inicio() >= RETENCAO_MS);
        }

        if (permitido.get()) return true;

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Retry-After", String.valueOf(Math.max(1, Duration.ofMillis(
                tentativas.get(chave).inicio() + JANELA_MS - agora).toSeconds())));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":429,\"mensagem\":\"Muitas tentativas. Aguarde alguns minutos e tente novamente.\"}");
        return false;
    }

    private Integer limitePara(String caminho, String metodo) {
        if ("/acompanhamento".equals(caminho)) {
            return "GET".equalsIgnoreCase(metodo) ? 10 : null;
        }
        if (!"POST".equalsIgnoreCase(metodo)) return null;
        if (caminho.endsWith("/login")) return 20;
        if (caminho.contains("forgot") || caminho.contains("esqueci") || caminho.contains("recuperar")) return 5;
        if (caminho.contains("reset") || caminho.contains("redefinir") || caminho.contains("senha")) return 10;
        return null;
    }

    private record Janela(long inicio, int quantidade) { }
}
