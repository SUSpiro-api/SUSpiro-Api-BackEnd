package com.app.suspiro.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;

class SecurityAuthTest {

    @Test
    void contaComBloqueioFuturoNaoPodeAutenticar() {
        UsuariosModel usuario = usuarioBase();
        ReflectionTestUtils.setField(usuario, "bloqueadoAte", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

        assertFalse(new SecurityAuth(usuario).isAccountNonLocked());
    }

    @Test
    void contaSemBloqueioOuComBloqueioExpiradoPodeAutenticar() {
        UsuariosModel semBloqueio = usuarioBase();
        UsuariosModel bloqueioExpirado = usuarioBase();
        ReflectionTestUtils.setField(bloqueioExpirado, "bloqueadoAte", LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));

        assertTrue(new SecurityAuth(semBloqueio).isAccountNonLocked());
        assertTrue(new SecurityAuth(bloqueioExpirado).isAccountNonLocked());
    }

    private UsuariosModel usuarioBase() {
        UsuariosModel usuario = new UsuariosModel();
        usuario.setCpf("52998224725");
        usuario.setNome("Usuário de teste");
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(Perfil.MEDICO);
        usuario.setAtivo(true);
        return usuario;
    }
}
