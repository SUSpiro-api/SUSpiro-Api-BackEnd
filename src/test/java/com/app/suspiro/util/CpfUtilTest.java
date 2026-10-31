package com.app.suspiro.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CpfUtilTest {
    @Test void normalizaCpfFormatado() {
        assertEquals("52998224725", CpfUtil.normalizar("529.982.247-25"));
    }
    @Test void aceitaCpfComDigitosVerificadoresValidos() {
        assertTrue(CpfUtil.valido("529.982.247-25"));
    }
    @Test void rejeitaCpfComDigitosInvalidosOuRepetidos() {
        assertFalse(CpfUtil.valido("52998224724"));
        assertFalse(CpfUtil.valido("11111111111"));
        assertFalse(CpfUtil.valido("123"));
        assertFalse(CpfUtil.valido("abc52998224725"));
    }
}
