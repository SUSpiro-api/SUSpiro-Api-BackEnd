package com.app.suspiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarNomeRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome,
        @Size(max = 30, message = "O sexo deve ter no máximo 30 caracteres") String sexo,
        @Size(max = 40, message = "O registro profissional deve ter no máximo 40 caracteres") String registroProfissional) {
}
