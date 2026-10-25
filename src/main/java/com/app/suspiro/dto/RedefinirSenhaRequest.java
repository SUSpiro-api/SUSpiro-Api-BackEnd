package com.app.suspiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank(message = "O token de recuperação é obrigatório")
        @Size(max = 200, message = "Token inválido")
        String token,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,72}$",
                message = "Use pelo menos 12 caracteres, com maiúscula, minúscula, número e símbolo"
        )
        String novaSenha) {
}
