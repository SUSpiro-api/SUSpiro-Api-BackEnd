package com.app.suspiro.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;

public interface UsuariosRepository extends JpaRepository<UsuariosModel, Long> {

    Optional<UsuariosModel> findByCpf(String cpf); 

    Optional<UsuariosModel> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);
}