package com.app.suspiro.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.suspiro.dto.UsuarioCadastroRequest;
import com.app.suspiro.dto.UsuarioResponse;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.security.SecurityAuth;
import com.app.suspiro.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE')")
    public ResponseEntity<UsuarioResponse> cadastrar(
            @Valid @RequestBody UsuarioCadastroRequest request,
            Authentication authentication) {

        SecurityAuth autenticado =
                (SecurityAuth) authentication.getPrincipal();

        UsuariosModel usuario = usuarioService.cadastrar(
                request,
                autenticado.getUsuario().getPerfil()
        );

        return ResponseEntity
                .created(URI.create("/usuarios/" + usuario.getId()))
                .body(UsuarioResponse.de(usuario));
    }
}
