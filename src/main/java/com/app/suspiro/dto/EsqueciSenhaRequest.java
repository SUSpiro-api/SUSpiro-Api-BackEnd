package com.app.suspiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EsqueciSenhaRequest(

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(
                regexp = "[0-9]{11}",
                message = "O CPF deve conter exatamente 11 dígitos numéricos"
        )
        String cpf) {
}
