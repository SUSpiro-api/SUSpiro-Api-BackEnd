package com.app.suspiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.model.GuiaModel;

public interface GuiaRepository extends JpaRepository<GuiaModel, Long> {

    List<GuiaModel> findBySolicitanteIdOrderByCriadoEmDesc(Long solicitanteId);

    List<GuiaModel> findByStatusOrderByPrioridadeDescCriadoEmAsc(StatusGuia status);

    List<GuiaModel> findByPacienteIdOrderByCriadoEmDescIdDesc(UUID pacienteId);

    Optional<GuiaModel> findByIdAndStatus(Long id, StatusGuia status);
}
