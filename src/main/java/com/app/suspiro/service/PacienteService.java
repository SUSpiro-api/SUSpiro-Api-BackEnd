package com.app.suspiro.service;

import java.util.UUID;

import com.app.suspiro.dto.PacienteCadastroRequest;
import com.app.suspiro.exception.RecursoNaoEncontradoException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.repository.PacientesRepository;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

    private final PacientesRepository pacientesRepository;

    public PacienteService(PacientesRepository pacientesRepository) {
        this.pacientesRepository = pacientesRepository;
    }

    public PacientesModel cadastrar(PacienteCadastroRequest request) {
        if (pacientesRepository.existsByCpf(request.getCpf())) {
            throw new RegraDeNegocioException("CPF já cadastrado.");
        }

        if (pacientesRepository.existsByNumeroSus(request.getNumeroSus())) {
            throw new RegraDeNegocioException("Número do SUS já cadastrado.");
        }

        PacientesModel paciente = new PacientesModel();
        paciente.setNome(request.getNome());
        paciente.setCpf(request.getCpf());
        paciente.setDataNascimento(request.getDataNascimento());
        paciente.setNumeroSus(request.getNumeroSus());
        paciente.setTelefone(request.getTelefone());

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            paciente.setEmail(request.getEmail().trim().toLowerCase());
        }

        return pacientesRepository.save(paciente);
    }

    public PacientesModel buscarPorId(UUID id) {
        return pacientesRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Paciente não encontrado.")
                );
    }

    public PacientesModel buscarPorCpf(String cpf) {
        return pacientesRepository.findByCpf(cpf)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Paciente não encontrado.")
                );
    }
}
