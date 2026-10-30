package com.app.suspiro.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> frontendOrigins;
    private final PublicEndpointRateLimitInterceptor publicEndpointRateLimitInterceptor;

    public CorsConfig(@Value("${FRONTEND_URL}") String frontendUrls,
            PublicEndpointRateLimitInterceptor publicEndpointRateLimitInterceptor) {
        this.publicEndpointRateLimitInterceptor = publicEndpointRateLimitInterceptor;
        this.frontendOrigins = Arrays.stream(frontendUrls.split(","))
                .map(String::trim)
                .map(origin -> origin.replaceAll("/+$", ""))
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();

        if (this.frontendOrigins.isEmpty()) {
            throw new IllegalStateException("Configure ao menos uma origem em FRONTEND_URL.");
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(publicEndpointRateLimitInterceptor)
                .addPathPatterns(
                        "/acompanhamento",
                        "/auth/**",
                        "/**/login",
                        "/**/forgot-password",
                        "/**/reset-password",
                        "/**/esqueci-senha",
                        "/**/redefinir-senha",
                        "/**/*senha*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Requested-With")
                .maxAge(3600);
    }
}
