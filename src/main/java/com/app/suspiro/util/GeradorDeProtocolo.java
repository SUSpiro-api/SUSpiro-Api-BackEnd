package com.app.suspiro.util;

import java.security.SecureRandom;

public final class GeradorDeProtocolo {

    public static final int TAMANHO = 10;
    private static final int TAMANHO_PROTOCOLO_GUIA = 12;

    private static final SecureRandom RANDOM = new SecureRandom();
    // Alfabeto sem caracteres visualmente ambíguos (0/O e 1/I).
    private static final char[] ALFABETO_GUIA = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private GeradorDeProtocolo() {
    }


    public static String gerarProtocoloGuia() {
        StringBuilder protocolo = new StringBuilder("SUS-");
        for (int i = 0; i < TAMANHO_PROTOCOLO_GUIA; i++) {
            protocolo.append(ALFABETO_GUIA[RANDOM.nextInt(ALFABETO_GUIA.length)]);
        }
        return protocolo.toString();
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
