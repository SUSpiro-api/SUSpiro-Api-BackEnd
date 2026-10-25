package com.app.suspiro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.model.RequisicoesModel;
import jakarta.persistence.LockModeType;

public interface RequisicoesRepository extends JpaRepository<RequisicoesModel, Long> {

    boolean existsByProtocolo(String protocolo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RequisicoesModel r where r.id = :id")
    Optional<RequisicoesModel> findByIdForUpdate(@Param("id") Long id);

    Optional<RequisicoesModel> findByProtocoloAndPacienteCpf(String protocolo, String cpf);

    List<RequisicoesModel> findByStatusAndDataVencimentoBefore(
            StatusRequisicao status, LocalDate data);

    List<RequisicoesModel> findByPacienteIdOrderByCriadoEmDesc(UUID pacienteId);

    @Query("""
            SELECT r FROM RequisicoesModel r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:pacienteId IS NULL OR r.paciente.id = :pacienteId)
              AND (:solicitanteId IS NULL OR r.solicitante.id = :solicitanteId)
            """)
    Page<RequisicoesModel> buscar(
            @Param("status") StatusRequisicao status,
            @Param("pacienteId") UUID pacienteId,
            @Param("solicitanteId") Long solicitanteId,
            Pageable paginacao);
}
