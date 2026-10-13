package com.app.suspiro.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraDeNegocio(
            RegraDeNegocioException ex) {

        return montarResposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(
            RecursoNaoEncontradoException ex) {

        return montarResposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> tratarAcessoNegado(
            AccessDeniedException ex) {

        return montarResposta(
                HttpStatus.FORBIDDEN,
                "Você não tem permissão para executar esta operação"
        );
    }

    @ExceptionHandler(EnvioDeEmailException.class)
    public ResponseEntity<Map<String, Object>> tratarEnvioDeEmail(
            EnvioDeEmailException ex) {

        return montarResposta(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage() + ". Nada foi alterado, tente novamente."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(
            MethodArgumentNotValidException ex) {

        Map<String, String> erros = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(erro ->
                erros.put(erro.getField(), erro.getDefaultMessage())
        );

        ResponseEntity<Map<String, Object>> resposta =
                montarResposta(HttpStatus.BAD_REQUEST, "Dados inválidos");

        resposta.getBody().put("erros", erros);

        return resposta;
    }

    private ResponseEntity<Map<String, Object>> montarResposta(
            HttpStatus status, String mensagem) {

        Map<String, Object> corpo = new LinkedHashMap<>();

        corpo.put("timestamp", LocalDateTime.now());
        corpo.put("status", status.value());
        corpo.put("mensagem", mensagem);

        return ResponseEntity.status(status).body(corpo);
    }
}
