package com.app.suspiro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.suspiro.dto.TrocaDeSenhaRequest;
import com.app.suspiro.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
public class SenhaController {

    private final UsuarioService usuarioService;

    public SenhaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PatchMapping("/usuarios/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(
            @Valid @RequestBody TrocaDeSenhaRequest request,
            Authentication authentication) {

        usuarioService.trocarSenha(authentication.getName(), request);
    }
}
