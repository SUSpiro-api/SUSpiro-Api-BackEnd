package com.app.suspiro.dto;

import java.time.LocalDate;
import java.util.UUID;
import com.app.suspiro.model.PacientesModel;

/** Resposta mínima para localizar o cadastro sem expor endereço, e-mail ou telefone na busca. */
public record PacienteIdentificacaoResponse(UUID id, String nome, String cpf, LocalDate dataNascimento) {
    public static PacienteIdentificacaoResponse de(PacientesModel paciente) {
        return new PacienteIdentificacaoResponse(
                paciente.getId(), paciente.getNome(), paciente.getCpf(), paciente.getDataNascimento());
    }
}
