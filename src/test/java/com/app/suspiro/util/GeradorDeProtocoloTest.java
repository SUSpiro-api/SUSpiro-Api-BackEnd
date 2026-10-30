package com.app.suspiro.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeradorDeProtocoloTest {

    @Test
    void geraProtocolosDeGuiaComPrefixoTamanhoEAlfabetoEsperados() {
        String protocolo = GeradorDeProtocolo.gerarProtocoloGuia();

        assertTrue(protocolo.matches("^SUS-[A-HJ-NP-Z2-9]{12}$"));
        assertEquals(16, protocolo.length());
    }

    @Test
    void protocolosGeradosNaoSaoNumerosSequenciaisEVariamEntreGuias() {
        Set<String> protocolos = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            protocolos.add(GeradorDeProtocolo.gerarProtocoloGuia());
        }
        assertEquals(1_000, protocolos.size());
    }
}
