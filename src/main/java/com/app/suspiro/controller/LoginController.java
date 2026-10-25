package com.app.suspiro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.suspiro.dto.EsqueciSenhaRequest;
import com.app.suspiro.dto.RedefinirSenhaRequest;
import com.app.suspiro.dto.UsuarioLoginRequest;
import com.app.suspiro.dto.UsuarioResponse;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.security.JwtService;
import com.app.suspiro.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public LoginController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioService usuarioService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody UsuarioLoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getCpf(),
                        request.getSenha()
                )
        );

        UsuariosModel usuario =
                usuarioService.buscarPorCpf(request.getCpf());

        String token =
                jwtService.gerarToken(usuario);

        return ResponseEntity.ok(
                new LoginResponse(token, usuario.isSenhaProvisoria(), UsuarioResponse.de(usuario))
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        usuarioService.revogarSessoesAtivas(authentication.getName());
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        usuarioService.redefinirSenha(request.cpf());
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        usuarioService.confirmarRedefinicaoSenha(request);
    }

    public record LoginResponse(String token, boolean senhaProvisoria, UsuarioResponse user) {
    }
}
