package com.app.suspiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.model.GuiaModel;

public interface GuiaRepository extends JpaRepository<GuiaModel, Long> {

    List<GuiaModel> findBySolicitanteIdOrderByCriadoEmDesc(Long solicitanteId);

    List<GuiaModel> findByStatusOrderByPrioridadeDescCriadoEmAsc(StatusGuia status);

    List<GuiaModel> findByPacienteIdOrderByCriadoEmDescIdDesc(UUID pacienteId);

    Optional<GuiaModel> findByIdAndStatus(Long id, StatusGuia status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GuiaModel g where g.id = :id")
    Optional<GuiaModel> findByIdForUpdate(@Param("id") Long id);
}
