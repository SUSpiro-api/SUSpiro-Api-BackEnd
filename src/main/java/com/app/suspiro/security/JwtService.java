package com.app.suspiro.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.app.suspiro.model.UsuariosModel;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final String jwtSecret;
    private final long jwtExpiration;

    public JwtService(
            @Value("${JWT_SECRET}") String jwtSecret,
            @Value("${JWT_EXPIRATION}") long jwtExpiration) {

        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }



    public String gerarToken(UsuariosModel usuario) {

        return Jwts.builder()

                .subject(usuario.getCpf())

                .claim(
                        "perfil",
                        usuario.getPerfil().name()
                )

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiration
                        )
                )

                .signWith(getSigningKey())

                .compact();
    }


    public String extrairCpf(String token) {

        return extrairClaims(token)
                .getSubject();
    }


    public Claims extrairClaims(String token) {

        return Jwts.parser()

                .verifyWith(getSigningKey())

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }


    public boolean tokenValido(String token) {

        try {

            extrairClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}