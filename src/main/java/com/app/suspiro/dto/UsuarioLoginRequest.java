package com.app.suspiro.dto;

import com.app.suspiro.util.CpfUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UsuarioLoginRequest {

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "[0-9]{11}", message = "O CPF deve conter 11 dígitos numéricos")
    private String cpf;

    @NotBlank(message = "A senha é obrigatória")
    private String senha;

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf == null ? null : CpfUtil.normalizar(cpf); }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
