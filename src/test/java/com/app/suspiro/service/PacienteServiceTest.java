package com.app.suspiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;

import com.app.suspiro.dto.PacienteCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.PacientesRepository;
import com.app.suspiro.repository.UsuariosRepository;

class PacienteServiceTest {

    private PacientesRepository pacientesRepository;
    private UsuariosRepository usuariosRepository;
    private PasswordEncoder passwordEncoder;
    private PacienteService service;

    @BeforeEach
    void setUp() {
        pacientesRepository = Mockito.mock(PacientesRepository.class);
        usuariosRepository = Mockito.mock(UsuariosRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new PacienteService(
                pacientesRepository,
                usuariosRepository,
                passwordEncoder);

        when(pacientesRepository.existsByCpf(anyString())).thenReturn(false);
        when(usuariosRepository.existsByCpf(anyString())).thenReturn(false);
        when(usuariosRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(pacientesRepository.save(any(PacientesModel.class)))
                .thenAnswer(invocation -> {
                    PacientesModel paciente = invocation.getArgument(0);
                    if (paciente.getId() == null) paciente.setId(UUID.randomUUID());
                    return paciente;
                });
        when(usuariosRepository.save(any(UsuariosModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void criaContaPacienteComDataDeNascimentoComoSenhaInicialEExigeTroca() {
        PacienteCadastroRequest request = new PacienteCadastroRequest();
        request.setNome("Maria da Silva");
        request.setCpf("52998224725");
        request.setDataNascimento(java.time.LocalDate.of(1990, 5, 20));
        request.setConvenio("Plano Exemplo");
        request.setTelefone("11999999999");
        request.setEmail(" Maria@Example.com ");

        PacientesModel paciente = service.cadastrar(request);

        ArgumentCaptor<UsuariosModel> contaCaptor = ArgumentCaptor.forClass(UsuariosModel.class);
        verify(usuariosRepository).save(contaCaptor.capture());
        UsuariosModel conta = contaCaptor.getValue();

        assertEquals("maria@example.com", paciente.getEmail());
        assertEquals("Plano Exemplo", paciente.getConvenio());
        assertEquals(Perfil.PACIENTE, conta.getPerfil());
        assertEquals(paciente.getId(), conta.getPacienteId(), "A conta precisa apontar para o UUID do paciente criado");
        assertTrue(conta.isAtivo());
        assertTrue(conta.isSenhaProvisoria());
        assertEquals("maria@example.com", conta.getEmail());
        assertTrue(passwordEncoder.matches("20051990", conta.getSenha()));
        assertFalse(conta.getSenha().equals("20051990"));
    }

}
