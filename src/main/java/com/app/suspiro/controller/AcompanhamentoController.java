package com.app.suspiro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.suspiro.dto.AcompanhamentoResponse;
import com.app.suspiro.service.RequisicaoService;

// Rota pública: o paciente não tem login, ele consulta com protocolo + CPF.
@RestController
@RequestMapping("/acompanhamento")
public class AcompanhamentoController {

    private final RequisicaoService requisicaoService;

    public AcompanhamentoController(RequisicaoService requisicaoService) {
        this.requisicaoService = requisicaoService;
    }

    @GetMapping
    public AcompanhamentoResponse acompanhar(
            @RequestParam String protocolo,
            @RequestParam String cpf) {

        return AcompanhamentoResponse.de(
                requisicaoService.acompanhar(protocolo.trim(), cpf.replaceAll("\\D", ""))
        );
    }
}
