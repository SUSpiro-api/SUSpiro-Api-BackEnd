package com.app.suspiro.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PacienteCadastroRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
    private String cpf;

    @NotNull(message = "Data de nascimento é obrigatória")
    private LocalDate dataNascimento;

    @NotBlank(message = "Número do SUS é obrigatório")
    @Pattern(regexp = "\\d{15}", message = "Número do SUS deve conter exatamente 15 dígitos")
    private String numeroSus;

    private String telefone;

    @Email(message = "E-mail inválido")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
    private String email;

    public String getNome() {
    return nome;
    }

    public void setNome(String nome) {
    this.nome = nome;
    }

    public String getCpf() {
	return cpf;
    }

    public void setCpf(String cpf) {
	this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
	return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
	this.dataNascimento = dataNascimento;
    }

    public String getNumeroSus() {
	return numeroSus;
    }

    public void setNumeroSus(String numeroSus) {
	this.numeroSus = numeroSus;
    }

    public String getTelefone() {
	return telefone;
    }

    public void setTelefone(String telefone) {
	this.telefone = telefone;
    }

    public String getEmail() {
	return email;
    }

    public void setEmail(String email) {
	this.email = email;
    }

    }
