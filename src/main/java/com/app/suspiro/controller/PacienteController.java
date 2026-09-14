package com.app.suspiro.controller;

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
    public ResponseEntity<PacientesModel> buscarPorId(@PathVariable Long id) {
        PacientesModel paciente = pacienteService.buscarPorId(id);

        return ResponseEntity.ok(paciente);
    }
}
