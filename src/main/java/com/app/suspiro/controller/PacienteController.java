package com.app.suspiro.controller;

import java.util.UUID;

import com.app.suspiro.dto.PacienteCadastroRequest;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    public ResponseEntity<PacientesModel> cadastrar(
            @Valid @RequestBody PacienteCadastroRequest request) {

        PacientesModel paciente = pacienteService.cadastrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paciente);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacientesModel> buscarPorId(@PathVariable UUID id) {
        PacientesModel paciente = pacienteService.buscarPorId(id);

        return ResponseEntity.ok(paciente);
    }

    @GetMapping(params = "cpf")
    public ResponseEntity<PacientesModel> buscarPorCpf(@RequestParam String cpf) {
        PacientesModel paciente = pacienteService.buscarPorCpf(cpf.replaceAll("\\D", ""));

        return ResponseEntity.ok(paciente);
    }
}
