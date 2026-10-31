package com.app.suspiro.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import com.app.suspiro.model.PacientesModel;

/** DTO explícito: não serializa a entidade JPA nem campos de autenticação. */
public record PacienteResponse(
        UUID id, String nome, String cpf, LocalDate dataNascimento, String sexo, String convenio, String numeroCarteirinha,
        String telefone, String email, String cep, String logradouro, String numero,
        String complemento, String bairro, String cidade, String estado, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
    public static PacienteResponse de(PacientesModel p) {
        return new PacienteResponse(p.getId(), p.getNome(), p.getCpf(), p.getDataNascimento(),
                p.getSexo(), p.getConvenio(), p.getNumeroCarteirinha(), p.getTelefone(), p.getEmail(), p.getCep(), p.getLogradouro(),
                p.getNumero(), p.getComplemento(), p.getBairro(), p.getCidade(), p.getEstado(), p.getCriadoEm(), p.getAtualizadoEm());
    }
}
