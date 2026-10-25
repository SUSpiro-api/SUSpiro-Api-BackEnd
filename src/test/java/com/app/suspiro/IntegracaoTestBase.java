package com.app.suspiro;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.app.suspiro.config.DotenvLoader;

/**
 * Base dos testes de integração. Usa o PostgreSQL configurado no .env, mas sempre num
 * banco exclusivo de testes (TEST_DB_NAME, padrão "suspiro_test"), nunca o DB_NAME.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegracaoTestBase {

    static {
        DotenvLoader.init();
    }

    @DynamicPropertySource
    static void bancoDeTeste(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://"
                + valor("TEST_DB_HOST", valor("DB_HOST", "localhost")) + ":"
                + valor("TEST_DB_PORT", valor("DB_PORT", "5432")) + "/"
                + valor("TEST_DB_NAME", "suspiro_test"));
    }

    private static String valor(String chave, String valorPadrao) {
        String valor = System.getenv(chave);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(chave);
        }
        return valor == null || valor.isBlank() ? valorPadrao : valor;
    }
}
