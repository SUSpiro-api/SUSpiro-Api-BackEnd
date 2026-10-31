package com.app.suspiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
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

    boolean existsByProtocolo(String protocolo);

    Optional<GuiaModel> findByProtocolo(String protocolo);

    @Query("""
            select g.id from GuiaModel g
            where g.status = :status
              and g.criadoEm <= :limite
              and (g.lembretePendenteEnviadoEm is null or g.lembretePendenteEnviadoEm <= :limite)
              and (g.lembretePendenteTentativaEm is null or g.lembretePendenteTentativaEm <= :tentativaApos)
              and g.id not in :ignorados
            order by case when g.lembretePendenteTentativaEm is null then 0 else 1 end,
                     g.lembretePendenteTentativaEm asc, g.criadoEm asc
            """)
    List<Long> buscarIdsDeGuiasQuePrecisamDeLembrete(
            @Param("status") StatusGuia status,
            @Param("limite") java.time.LocalDateTime limite,
            @Param("tentativaApos") java.time.LocalDateTime tentativaApos,
            @Param("ignorados") List<Long> ignorados,
            Pageable pageable);

    @Query("""
            select g.id from GuiaModel g
            where g.status = :status
              and g.notificacaoAgendamentoEnviadaEm is null
              and (g.notificacaoAgendamentoTentativaEm is null or g.notificacaoAgendamentoTentativaEm <= :tentativaApos)
              and g.id not in :ignorados
            order by case when g.notificacaoAgendamentoTentativaEm is null then 0 else 1 end,
                     g.notificacaoAgendamentoTentativaEm asc, g.atualizadoEm asc
            """)
    List<Long> buscarIdsDeAgendamentosSemConfirmacao(
            @Param("status") StatusGuia status,
            @Param("tentativaApos") java.time.LocalDateTime tentativaApos,
            @Param("ignorados") List<Long> ignorados,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GuiaModel g where g.id = :id")
    Optional<GuiaModel> findByIdForUpdate(@Param("id") Long id);
}
