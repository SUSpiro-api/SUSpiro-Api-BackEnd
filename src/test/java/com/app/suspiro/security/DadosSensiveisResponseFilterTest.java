package com.app.suspiro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DadosSensiveisResponseFilterTest {

    private final DadosSensiveisResponseFilter filter = new DadosSensiveisResponseFilter();

    @Test
    void impedeCacheDeRotasDeRequisicoes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/requisicoes/minhas");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("no-store, no-cache, must-revalidate, private", response.getHeader("Cache-Control"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
    }

    @Test
    void naoAlteraCabecalhosDeRotasPublicasSemDadosSensiveis() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader("Cache-Control"));
    }
}
