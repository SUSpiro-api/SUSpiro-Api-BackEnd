package com.app.suspiro.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.suspiro.model.PacientesModel;

public record PacienteCadastroResponse(
        UUID id,
        String nome,
        String cpfMascarado,
        String convenio,
        String email,
        LocalDateTime criadoEm,
        boolean contaCriada) {

    public static PacienteCadastroResponse de(PacientesModel paciente) {
        String cpf = paciente.getCpf();
        String cpfMascarado = cpf.length() == 11
                ? "•••.•••.•••-" + cpf.substring(9)
                : "CPF cadastrado";

        return new PacienteCadastroResponse(
                paciente.getId(),
                paciente.getNome(),
                cpfMascarado,
                paciente.getConvenio(),
                paciente.getEmail(),
                paciente.getCriadoEm(),
                true
        );
    }
}
