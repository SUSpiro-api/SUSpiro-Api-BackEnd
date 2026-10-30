package com.app.suspiro.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;

class AdminRootTest {

    private static final String CPF_VALIDO = "52998224725";

    @Test
    void naoPromoveOutraContaParaSuperAdminDuranteInicializacao() {
        UsuariosRepository repository = mock(UsuariosRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UsuariosModel paciente = new UsuariosModel();
        paciente.setPerfil(Perfil.PACIENTE);
        when(repository.findByCpf(CPF_VALIDO)).thenReturn(Optional.of(paciente));

        AdminRoot adminRoot = new AdminRoot(
                repository, passwordEncoder, CPF_VALIDO, "Administrador", "SenhaSegura#2026");

        assertThrows(IllegalStateException.class, adminRoot::run);
        verify(repository, never()).save(any(UsuariosModel.class));
    }

    @Test
    void mantemContaSuperAdminConfiguradaSemAlteracoes() {
        UsuariosRepository repository = mock(UsuariosRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UsuariosModel admin = new UsuariosModel();
        admin.setPerfil(Perfil.SUPER_ADMIN);
        when(repository.findByCpf(CPF_VALIDO)).thenReturn(Optional.of(admin));

        AdminRoot adminRoot = new AdminRoot(
                repository, passwordEncoder, CPF_VALIDO, "Administrador", "SenhaSegura#2026");

        assertDoesNotThrow((Executable) () -> adminRoot.run(new String[0]));
        verify(repository, never()).save(any(UsuariosModel.class));
    }
}
