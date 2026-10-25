package com.app.suspiro.dto;

import java.time.LocalDate;

import com.app.suspiro.util.CpfUtil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PacienteCadastroRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
    private String cpf;

    @NotNull(message = "Data de nascimento é obrigatória")
    @PastOrPresent(message = "A data de nascimento não pode estar no futuro")
    private LocalDate dataNascimento;

    @Size(max = 120, message = "O convênio deve ter no máximo 120 caracteres")
    private String convenio;

    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @NotBlank(message = "O e-mail é obrigatório para recuperação de senha")
    @Email(message = "E-mail inválido")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
    private String email;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf == null ? null : CpfUtil.normalizar(cpf); }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getConvenio() { return convenio; }
    public void setConvenio(String convenio) { this.convenio = convenio; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
