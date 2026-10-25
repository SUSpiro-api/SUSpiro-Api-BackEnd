package com.app.suspiro.controller;

import java.util.List;
import java.util.UUID;

import com.app.suspiro.dto.PacienteCadastroRequest;
import com.app.suspiro.dto.PacienteCadastroResponse;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private static final String PROFISSIONAIS = "hasAnyRole('SUPER_ADMIN', 'GERENTE', 'MEDICO', 'ENFERMEIRO', 'ATENDENTE')";
    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    @PreAuthorize(PROFISSIONAIS)
    public ResponseEntity<PacienteCadastroResponse> cadastrar(
            @Valid @RequestBody PacienteCadastroRequest request) {
        PacientesModel paciente = pacienteService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteCadastroResponse.de(paciente));
    }

    @GetMapping("/{id}")
    @PreAuthorize(PROFISSIONAIS)
    public ResponseEntity<PacientesModel> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }

    @GetMapping(params = "cpf")
    @PreAuthorize(PROFISSIONAIS)
    public ResponseEntity<PacientesModel> buscarPorCpf(@RequestParam String cpf) {
        return ResponseEntity.ok(pacienteService.buscarPorCpf(cpf));
    }

    @GetMapping(params = "busca")
    @PreAuthorize(PROFISSIONAIS)
    public ResponseEntity<List<PacientesModel>> buscar(@RequestParam("busca") String termo) {
        return ResponseEntity.ok(pacienteService.buscarPorTexto(termo));
    }
}
