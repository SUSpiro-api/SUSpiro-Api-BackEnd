package com.app.suspiro.util;

import java.security.SecureRandom;

public final class GeradorDeProtocolo {

    public static final int TAMANHO = 10;

    private static final SecureRandom RANDOM = new SecureRandom();

    private GeradorDeProtocolo() {
    }


    public static String gerar() {

        StringBuilder protocolo = new StringBuilder(TAMANHO);

        protocolo.append(1 + RANDOM.nextInt(9));

        while (protocolo.length() < TAMANHO) {
            protocolo.append(RANDOM.nextInt(10));
        }

        return protocolo.toString();
    }
}
