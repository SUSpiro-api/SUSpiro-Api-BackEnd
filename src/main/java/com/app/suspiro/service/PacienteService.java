package com.app.suspiro.service;

import com.app.suspiro.dto.PacienteCadastroRequest;
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
            throw new IllegalArgumentException("CPF já cadastrado.");
        }

        if (pacientesRepository.existsByNumeroSus(request.getNumeroSus())) {
            throw new IllegalArgumentException("Número do SUS já cadastrado.");
        }

        PacientesModel paciente = new PacientesModel();
        paciente.setNome(request.getNome());
        paciente.setCpf(request.getCpf());
        paciente.setDataNascimento(request.getDataNascimento());
        paciente.setNumeroSus(request.getNumeroSus());
        paciente.setTelefone(request.getTelefone());

        return pacientesRepository.save(paciente);
    }

    public PacientesModel buscarPorId(Long id) {
        return pacientesRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Paciente não encontrado.")
                );
    }
}
