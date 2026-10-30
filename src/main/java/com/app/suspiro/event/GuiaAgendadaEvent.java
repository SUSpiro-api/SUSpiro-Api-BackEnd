package com.app.suspiro.event;

/** Publicado na transação de agendamento; o envio ocorre somente depois do commit. */
public record GuiaAgendadaEvent(Long guiaId) {}
