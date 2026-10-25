package com.app.suspiro.repository;

import java.util.Collection;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;

public interface UsuariosRepository extends JpaRepository<UsuariosModel, Long> {

    Page<UsuariosModel> findByPerfilIn(Collection<Perfil> perfis, Pageable paginacao);

    Optional<UsuariosModel> findByCpf(String cpf); 

    Optional<UsuariosModel> findByEmail(String email);

    Optional<UsuariosModel> findByTokenRedefinicaoSenhaHashAndTokenRedefinicaoSenhaExpiraEmAfter(
            String tokenRedefinicaoSenhaHash, LocalDateTime agora);

    boolean existsByCpf(String cpf);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfil(Perfil perfil);
}