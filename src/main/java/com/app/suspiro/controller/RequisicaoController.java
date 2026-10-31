package com.app.suspiro.controller;

import java.net.URI;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.suspiro.dto.AgendamentoRequest;
import com.app.suspiro.dto.RequisicaoCadastroRequest;
import com.app.suspiro.dto.RequisicaoResponse;
import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.model.RequisicoesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.security.SecurityAuth;
import com.app.suspiro.service.RequisicaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/requisicoes")
public class RequisicaoController {

    private final RequisicaoService requisicaoService;

    public RequisicaoController(RequisicaoService requisicaoService) {
        this.requisicaoService = requisicaoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'MEDICO', 'ENFERMEIRO')")
    public ResponseEntity<RequisicaoResponse> cadastrar(
            @Valid @RequestBody RequisicaoCadastroRequest request,
            Authentication authentication) {

        RequisicoesModel requisicao =
                requisicaoService.cadastrar(request, usuarioLogado(authentication));

        return ResponseEntity
                .created(URI.create("/requisicoes/" + requisicao.getId()))
                .body(RequisicaoResponse.de(requisicao));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<RequisicaoResponse> listarMinhas(Authentication authentication) {
        UsuariosModel usuario = usuarioLogado(authentication);
        return requisicaoService.listarDoPaciente(usuario.getPacienteId())
                .stream().map(RequisicaoResponse::de).toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'MEDICO', 'ENFERMEIRO', 'ATENDENTE')")
    public Page<RequisicaoResponse> listar(
            @RequestParam(required = false) StatusRequisicao status,
            @RequestParam(required = false) UUID pacienteId,
            @PageableDefault(size = 20, sort = "dataVencimento", direction = Sort.Direction.ASC)
            Pageable paginacao,
            Authentication authentication) {

        return requisicaoService
                .listar(status, pacienteId, usuarioLogado(authentication), paginacao)
                .map(RequisicaoResponse::de);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'MEDICO', 'ENFERMEIRO', 'ATENDENTE')")
    public RequisicaoResponse buscarPorId(
            @PathVariable Long id,
            Authentication authentication) {

        return RequisicaoResponse.de(
                requisicaoService.buscarPorId(id, usuarioLogado(authentication))
        );
    }

    @PostMapping("/{id}/agendamento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'ATENDENTE')")
    public RequisicaoResponse agendar(
            @PathVariable Long id,
            @Valid @RequestBody AgendamentoRequest request,
            Authentication authentication) {

        return RequisicaoResponse.de(
                requisicaoService.agendar(id, request, usuarioLogado(authentication))
        );
    }

    @PatchMapping("/{id}/conclusao")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'ATENDENTE')")
    public RequisicaoResponse concluir(@PathVariable Long id) {

        return RequisicaoResponse.de(requisicaoService.concluir(id));
    }

    private UsuariosModel usuarioLogado(Authentication authentication) {

        return ((SecurityAuth) authentication.getPrincipal()).getUsuario();
    }
}
