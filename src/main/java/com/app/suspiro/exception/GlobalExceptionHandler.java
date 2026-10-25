package com.app.suspiro.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> tratarAutenticacao(AuthenticationException ex) {
        return montarResposta(HttpStatus.UNAUTHORIZED, "CPF ou senha inválidos.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return montarResposta(HttpStatus.BAD_REQUEST,
                "O corpo da requisição contém dados inválidos ou em formato incorreto.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return montarResposta(HttpStatus.BAD_REQUEST,
                "Um parâmetro da requisição está em formato inválido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> tratarParametroAusente(MissingServletRequestParameterException ex) {
        return montarResposta(HttpStatus.BAD_REQUEST,
                "Falta um parâmetro obrigatório na requisição.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarConflitoPersistencia(DataIntegrityViolationException ex) {
        return montarResposta(HttpStatus.CONFLICT,
                "A operação conflita com dados existentes. Verifique duplicidades e vínculos.");
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<Map<String, Object>> tratarDadosInvalidos(DadosInvalidosException ex) {
        return montarResposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

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
                "Não foi possível concluir o envio de e-mail. Nada foi alterado; tente novamente."
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroInterno(Exception ex) {
        // O tipo da exceção é suficiente para classificação; não registrar CPF, corpo ou token.
        log.error("Falha interna na API (tipo={})", ex.getClass().getSimpleName());
        return montarResposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "O servidor encontrou um erro inesperado. Tente novamente mais tarde.");
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
