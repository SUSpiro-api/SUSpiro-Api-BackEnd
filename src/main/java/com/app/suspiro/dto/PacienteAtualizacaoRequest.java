package com.app.suspiro.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Atualização integral dos dados editáveis; CPF é alterado apenas por operação administrativa separada. */
public class PacienteAtualizacaoRequest {
    @NotBlank @Size(max = 150) private String nome;
    @NotNull @PastOrPresent private LocalDate dataNascimento;
    @Size(max = 30) private String sexo;
    @Size(max = 120) private String convenio;
    @Size(max = 80) private String numeroCarteirinha;
    @Size(max = 20) private String telefone;
    @NotBlank @Email @Size(max = 255) private String email;
    @NotBlank @Pattern(regexp = "\\d{8}") private String cep;
    @NotBlank @Size(max = 180) private String logradouro;
    @NotBlank @Size(max = 20) private String numero;
    @Size(max = 120) private String complemento;
    @NotBlank @Size(max = 120) private String bairro;
    @NotBlank @Size(max = 120) private String cidade;
    @NotBlank @Pattern(regexp = "(?i)[A-Z]{2}") private String estado;

    public String getNome() { return nome; } public void setNome(String v) { nome = v; }
    public LocalDate getDataNascimento() { return dataNascimento; } public void setDataNascimento(LocalDate v) { dataNascimento = v; }
    public String getSexo() { return sexo; } public void setSexo(String v) { sexo = v; }
    public String getConvenio() { return convenio; } public void setConvenio(String v) { convenio = v; }
    public String getNumeroCarteirinha() { return numeroCarteirinha; } public void setNumeroCarteirinha(String v) { numeroCarteirinha = v; }
    public String getTelefone() { return telefone; } public void setTelefone(String v) { telefone = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getCep() { return cep; } public void setCep(String v) { cep = v == null ? null : v.replaceAll("\\D", ""); }
    public String getLogradouro() { return logradouro; } public void setLogradouro(String v) { logradouro = v; }
    public String getNumero() { return numero; } public void setNumero(String v) { numero = v; }
    public String getComplemento() { return complemento; } public void setComplemento(String v) { complemento = v; }
    public String getBairro() { return bairro; } public void setBairro(String v) { bairro = v; }
    public String getCidade() { return cidade; } public void setCidade(String v) { cidade = v; }
    public String getEstado() { return estado; } public void setEstado(String v) { estado = v == null ? null : v.trim().toUpperCase(java.util.Locale.ROOT); }
}
