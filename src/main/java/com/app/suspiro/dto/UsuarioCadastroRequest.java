package com.app.suspiro.dto;

import com.app.suspiro.enums.Perfil;

import com.app.suspiro.util.CpfUtil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioCadastroRequest {

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(
            regexp = "[0-9]{11}",
            message = "O CPF deve conter exatamente 11 dígitos numéricos"
    )
    private String cpf;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
    private String email;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotNull(message = "O perfil é obrigatório")
    private Perfil perfil;

    @NotBlank(message = "A senha temporária é obrigatória")
    @Size(min = 8, max = 72, message = "A senha temporária deve ter entre 8 e 72 caracteres")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "A senha temporária deve conter pelo menos uma letra e um número"
    )
    private String senhaTemporaria;

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf == null ? null : CpfUtil.normalizar(cpf);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public String getSenhaTemporaria() {
        return senhaTemporaria;
    }

    public void setSenhaTemporaria(String senhaTemporaria) {
        this.senhaTemporaria = senhaTemporaria;
    }
}
