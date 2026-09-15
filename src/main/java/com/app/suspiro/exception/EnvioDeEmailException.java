package com.app.suspiro.exception;

public class EnvioDeEmailException extends RuntimeException {

    public EnvioDeEmailException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
