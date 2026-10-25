package com.app.suspiro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.suspiro.dto.TrocaDeSenhaRequest;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.security.JwtService;
import com.app.suspiro.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
public class SenhaController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public SenhaController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PatchMapping("/usuarios/me/senha")
    public ResponseEntity<TrocaSenhaResponse> trocarSenha(
            @Valid @RequestBody TrocaDeSenhaRequest request,
            Authentication authentication) {

        UsuariosModel atualizado = usuarioService.trocarSenha(authentication.getName(), request);
        return ResponseEntity.ok(new TrocaSenhaResponse(jwtService.gerarToken(atualizado), false));
    }

    public record TrocaSenhaResponse(String token, boolean senhaProvisoria) {}
}
