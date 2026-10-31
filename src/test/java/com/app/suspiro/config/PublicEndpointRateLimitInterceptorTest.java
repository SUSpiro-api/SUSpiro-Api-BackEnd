package com.app.suspiro.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicEndpointRateLimitInterceptorTest {

    @Test
    void limitaConsultasPublicasPorEnderecoDeOrigem() throws Exception {
        PublicEndpointRateLimitInterceptor interceptor = new PublicEndpointRateLimitInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/acompanhamento");
        request.setRemoteAddr("203.0.113.10");

        for (int tentativa = 0; tentativa < 10; tentativa++) {
            assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(429, response.getStatus());
        assertTrue(Integer.parseInt(response.getHeader("Retry-After")) > 0);
    }

    @Test
    void naoLimitaRotasQueNaoEstaoNaListaPublica() throws Exception {
        PublicEndpointRateLimitInterceptor interceptor = new PublicEndpointRateLimitInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/guias/minhas");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
