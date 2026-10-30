package com.app.suspiro.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GuiaAgendamentoRequest {
    @NotNull
    private LocalDate dataAgendamento;

    @NotNull
    private LocalTime horaAgendamento;

    @NotBlank(message = "Informe o nome da unidade de saúde")
    @Size(max = 180, message = "O nome da unidade é inválido")
    private String estabelecimentoNome;

    @Size(max = 300, message = "O endereço da unidade é inválido")
    private String estabelecimentoEndereco;

    @Size(max = 120, message = "A cidade da unidade é inválida")
    private String estabelecimentoCidade;

    @Pattern(regexp = "(?i)^[A-Z]{2}$", message = "Informe uma UF com duas letras")
    private String estabelecimentoEstado;

    public LocalDate getDataAgendamento() { return dataAgendamento; }
    public void setDataAgendamento(LocalDate dataAgendamento) { this.dataAgendamento = dataAgendamento; }
    public LocalTime getHoraAgendamento() { return horaAgendamento; }
    public void setHoraAgendamento(LocalTime horaAgendamento) { this.horaAgendamento = horaAgendamento; }
    public String getEstabelecimentoNome() { return estabelecimentoNome; }
    public void setEstabelecimentoNome(String estabelecimentoNome) { this.estabelecimentoNome = estabelecimentoNome; }
    public String getEstabelecimentoEndereco() { return estabelecimentoEndereco; }
    public void setEstabelecimentoEndereco(String estabelecimentoEndereco) { this.estabelecimentoEndereco = estabelecimentoEndereco; }
    public String getEstabelecimentoCidade() { return estabelecimentoCidade; }
    public void setEstabelecimentoCidade(String estabelecimentoCidade) { this.estabelecimentoCidade = estabelecimentoCidade; }
    public String getEstabelecimentoEstado() { return estabelecimentoEstado; }
    public void setEstabelecimentoEstado(String estabelecimentoEstado) { this.estabelecimentoEstado = estabelecimentoEstado; }
}
