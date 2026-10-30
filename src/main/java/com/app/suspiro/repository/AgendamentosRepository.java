package com.app.suspiro.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.suspiro.model.AgendamentosModel;

public interface AgendamentosRepository extends JpaRepository<AgendamentosModel, Long> {

    boolean existsByRequisicaoId(Long requisicaoId);
}
