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
    private final long jwtFirstAccessExpiration;

    public JwtService(
            @Value("${JWT_SECRET}") String jwtSecret,
            @Value("${JWT_EXPIRATION}") long jwtExpiration,
            @Value("${JWT_FIRST_ACCESS_EXPIRATION_MS:600000}") long jwtFirstAccessExpiration) {

        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes; configure um segredo forte no ambiente.");
        }
        if (jwtExpiration <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION deve ser um número positivo de milissegundos.");
        }
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
        if (jwtFirstAccessExpiration <= 0 || jwtFirstAccessExpiration > 1800000) {
            throw new IllegalStateException("JWT_FIRST_ACCESS_EXPIRATION_MS deve estar entre 1 e 1800000 ms.");
        }
        this.jwtFirstAccessExpiration = jwtFirstAccessExpiration;
    }

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }



    public String gerarToken(UsuariosModel usuario) {

        return Jwts.builder()

                .subject(usuario.getCpf())

                .claim("perfil", usuario.getPerfil().name())
                .claim("ver", usuario.getTokenVersion())

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


    /** Token de no máximo 10 minutos para o fluxo restrito de primeira troca de senha. */
    public String gerarTokenDePrimeiroAcesso(UsuariosModel usuario) {
        if (!usuario.isSenhaProvisoria()) {
            throw new IllegalStateException("A conta não está em estado de primeiro acesso.");
        }
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(usuario.getCpf())
                .claim("perfil", usuario.getPerfil().name())
                .claim("ver", usuario.getTokenVersion())
                .claim("purpose", "FIRST_ACCESS")
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + jwtFirstAccessExpiration))
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


    /** Indica que o JWT foi emitido exclusivamente para ativação inicial de paciente. */
    public boolean tokenDePrimeiroAcesso(String token) {
        try {
            return "FIRST_ACCESS".equals(extrairClaims(token).get("purpose", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean tokenVersaoValida(String token, long versaoAtual) {
        try {
            Object claim = extrairClaims(token).get("ver");
            long versaoToken = claim instanceof Number numero ? numero.longValue() : 0L;
            return versaoToken == versaoAtual;
        } catch (Exception e) {
            return false;
        }
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