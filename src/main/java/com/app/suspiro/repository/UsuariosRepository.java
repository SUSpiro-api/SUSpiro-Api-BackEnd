package com.app.suspiro.repository;

import java.util.Collection;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.suspiro.enums.Perfil;
import java.util.UUID;
import com.app.suspiro.model.UsuariosModel;

public interface UsuariosRepository extends JpaRepository<UsuariosModel, Long> {

    Page<UsuariosModel> findByPerfilIn(Collection<Perfil> perfis, Pageable paginacao);

    Optional<UsuariosModel> findByCpf(String cpf); 

    Optional<UsuariosModel> findByEmail(String email);

    Optional<UsuariosModel> findByEmailIgnoreCase(String email);

    Optional<UsuariosModel> findByPacienteIdAndPerfil(UUID pacienteId, Perfil perfil);

    Optional<UsuariosModel> findByTokenRedefinicaoSenhaHashAndTokenRedefinicaoSenhaExpiraEmAfter(
            String tokenRedefinicaoSenhaHash, LocalDateTime agora);

    boolean existsByCpf(String cpf);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfil(Perfil perfil);

    /** Clears a lockout only after its expiry; safe across multiple application instances. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE usuarios
               SET tentativas_login_falhas = 0,
                   bloqueado_ate = NULL,
                   atualizado_em = CURRENT_TIMESTAMP
             WHERE cpf = :cpf
               AND bloqueado_ate IS NOT NULL
               AND bloqueado_ate <= :agora
            """, nativeQuery = true)
    int limparBloqueioExpirado(@Param("cpf") String cpf, @Param("agora") LocalDateTime agora);

    /** Atomically records a failed password attempt and locks the account at the configured threshold. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE usuarios
               SET tentativas_login_falhas = CASE
                       WHEN tentativas_login_falhas + 1 >= :limite THEN 0
                       ELSE tentativas_login_falhas + 1
                   END,
                   bloqueado_ate = CASE
                       WHEN tentativas_login_falhas + 1 >= :limite THEN :bloqueadoAte
                       ELSE bloqueado_ate
                   END,
                   atualizado_em = CURRENT_TIMESTAMP
             WHERE cpf = :cpf
               AND (bloqueado_ate IS NULL OR bloqueado_ate <= :agora)
            """, nativeQuery = true)
    int registrarFalhaLogin(
            @Param("cpf") String cpf,
            @Param("limite") int limite,
            @Param("agora") LocalDateTime agora,
            @Param("bloqueadoAte") LocalDateTime bloqueadoAte);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE usuarios
               SET tentativas_login_falhas = 0,
                   bloqueado_ate = NULL,
                   atualizado_em = CURRENT_TIMESTAMP
             WHERE cpf = :cpf
            """, nativeQuery = true)
    int registrarLoginBemSucedido(@Param("cpf") String cpf);
}