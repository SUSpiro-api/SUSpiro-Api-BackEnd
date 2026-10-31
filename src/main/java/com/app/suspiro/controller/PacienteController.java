package com.app.suspiro.controller;

import java.util.List;
import java.util.UUID;
import com.app.suspiro.dto.AlterarCpfPacienteRequest;
import com.app.suspiro.dto.PacienteAtualizacaoRequest;
import com.app.suspiro.dto.PacienteCadastroRequest;
import com.app.suspiro.dto.PacienteCadastroResponse;
import com.app.suspiro.dto.PacienteResponse;
import com.app.suspiro.dto.PacienteIdentificacaoResponse;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.security.SecurityAuth;
import com.app.suspiro.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {
    private static final String PROFISSIONAIS = "hasAnyRole('SUPER_ADMIN', 'GERENTE', 'MEDICO', 'ENFERMEIRO', 'ATENDENTE')";
    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) { this.pacienteService = pacienteService; }

    @PostMapping
    @PreAuthorize(PROFISSIONAIS)
    public ResponseEntity<PacienteCadastroResponse> cadastrar(@Valid @RequestBody PacienteCadastroRequest request) {
        PacientesModel paciente = pacienteService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteCadastroResponse.de(paciente));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public PacienteResponse meusDados(Authentication authentication) {
        UsuariosModel usuario = usuarioLogado(authentication);
        if (usuario.getPacienteId() == null) throw new RecursoNaoEncontradoException("Conta sem vínculo com cadastro de paciente.");
        return PacienteResponse.de(pacienteService.buscarPorId(usuario.getPacienteId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public PacienteResponse atualizarMeusDados(@Valid @RequestBody PacienteAtualizacaoRequest request, Authentication authentication) {
        UsuariosModel usuario = usuarioLogado(authentication);
        if (usuario.getPacienteId() == null) throw new RecursoNaoEncontradoException("Conta sem vínculo com cadastro de paciente.");
        return PacienteResponse.de(pacienteService.atualizar(usuario.getPacienteId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize(PROFISSIONAIS)
    public PacienteResponse buscarPorId(@PathVariable UUID id) { return PacienteResponse.de(pacienteService.buscarPorId(id)); }

    @PutMapping("/{id}")
    @PreAuthorize(PROFISSIONAIS)
    public PacienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PacienteAtualizacaoRequest request) {
        return PacienteResponse.de(pacienteService.atualizar(id, request));
    }

    @PatchMapping("/{id}/cpf")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PacienteResponse alterarCpf(@PathVariable UUID id, @Valid @RequestBody AlterarCpfPacienteRequest request) {
        return PacienteResponse.de(pacienteService.alterarCpf(id, request.cpf()));
    }

    @GetMapping(params = "cpf")
    @PreAuthorize(PROFISSIONAIS)
    public PacienteIdentificacaoResponse buscarPorCpf(@RequestParam String cpf) {
        return PacienteIdentificacaoResponse.de(pacienteService.buscarPorCpf(cpf));
    }

    @GetMapping(params = "busca")
    @PreAuthorize(PROFISSIONAIS)
    public List<PacienteIdentificacaoResponse> buscar(@RequestParam("busca") String termo) {
        return pacienteService.buscarPorTexto(termo).stream().map(PacienteIdentificacaoResponse::de).toList();
    }

    private UsuariosModel usuarioLogado(Authentication authentication) {
        return ((SecurityAuth) authentication.getPrincipal()).getUsuario();
    }
}
