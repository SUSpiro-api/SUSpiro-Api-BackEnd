package com.app.suspiro.repository;

import com.app.suspiro.model.PacientesModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PacientesRepository extends JpaRepository<PacientesModel, UUID> {

    boolean existsByCpf(String cpf);

    Optional<PacientesModel> findByCpf(String cpf);

    @Query("""
            SELECT p FROM PacientesModel p
            WHERE (:termo <> '' AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%')))
               OR (:cpf <> '' AND p.cpf LIKE CONCAT('%', :cpf, '%'))
            ORDER BY p.nome ASC
            """)
    List<PacientesModel> buscarPorNomeOuCpf(
            @Param("termo") String termo,
            @Param("cpf") String cpf,
            Pageable pageable);
}
