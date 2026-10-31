package com.app.suspiro.event;

/** Publicado sempre que uma requisição muda de estado; e-mail ocorre após o commit. */
public record RequisicaoStatusAlteradoEvent(Long requisicaoId) {}
