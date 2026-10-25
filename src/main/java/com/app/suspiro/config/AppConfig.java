package com.app.suspiro.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableScheduling
public class AppConfig {

    public static final String FUSO_HORARIO = "America/Sao_Paulo";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

  
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(FUSO_HORARIO));
    }
}
