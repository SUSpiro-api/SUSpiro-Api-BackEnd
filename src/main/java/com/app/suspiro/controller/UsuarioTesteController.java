package com.app.suspiro.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teste")
public class UsuarioTesteController {

    @GetMapping("/usuario")
    public String usuarioLogado(Authentication authentication) {

        return "Usuário logado: " + authentication.getName();
    }
}