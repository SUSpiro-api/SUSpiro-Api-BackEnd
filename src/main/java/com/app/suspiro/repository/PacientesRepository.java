package com.app.suspiro.repository;

import com.app.suspiro.model.PacientesModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PacientesRepository extends JpaRepository<PacientesModel, UUID> {

    boolean existsByCpf(String cpf);

    boolean existsByNumeroSus(String numeroSus);

    Optional<PacientesModel> findByCpf(String cpf);
}
