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
import java.util.Optional;
import java.time.LocalDate;
import com.app.suspiro.dto.PacienteAtualizacaoRequest;

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
    private EmailService emailService;
    private PacienteService service;

    @BeforeEach
    void setUp() {
        pacientesRepository = Mockito.mock(PacientesRepository.class);
        usuariosRepository = Mockito.mock(UsuariosRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        emailService = Mockito.mock(EmailService.class);
        service = new PacienteService(
                pacientesRepository,
                usuariosRepository,
                passwordEncoder,
                emailService);

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
    void criaContaPacienteComSenhaTemporariaPorEmailEExigeTrocaNoPrimeiroLogin() {
        PacienteCadastroRequest request = new PacienteCadastroRequest();
        request.setNome("Maria da Silva");
        request.setCpf("52998224725");
        request.setDataNascimento(java.time.LocalDate.of(1990, 5, 20));
        request.setConvenio("Plano Exemplo");
        request.setTelefone("11999999999");
        request.setEmail(" Maria@Example.com ");
        request.setCep("01001000");
        request.setLogradouro("Praça da Sé");
        request.setNumero("100");
        request.setComplemento("Sala 2");
        request.setBairro("Sé");
        request.setCidade("São Paulo");
        request.setEstado("sp");

        PacientesModel paciente = service.cadastrar(request);

        ArgumentCaptor<UsuariosModel> contaCaptor = ArgumentCaptor.forClass(UsuariosModel.class);
        verify(usuariosRepository).save(contaCaptor.capture());
        UsuariosModel conta = contaCaptor.getValue();

        assertEquals("maria@example.com", paciente.getEmail());
        assertEquals("Plano Exemplo", paciente.getConvenio());
        assertEquals("01001000", paciente.getCep());
        assertEquals("São Paulo", paciente.getCidade());
        assertEquals("SP", paciente.getEstado());
        assertEquals(Perfil.PACIENTE, conta.getPerfil());
        assertEquals(paciente.getId(), conta.getPacienteId(), "A conta precisa apontar para o UUID do paciente criado");
        assertTrue(conta.isAtivo());
        assertTrue(conta.isSenhaProvisoria());
        assertEquals("maria@example.com", conta.getEmail());
        assertFalse(passwordEncoder.matches("20051990", conta.getSenha()),
                "A data de nascimento não pode ser usada como senha inicial");
        assertTrue(conta.getSenha().startsWith("$2"),
                "A senha persistida deve ser um hash BCrypt, não a senha em texto puro");
        verify(emailService).enviarSenhaTemporariaPaciente(Mockito.eq(conta), Mockito.argThat(senha ->
                senha != null && senha.length() >= 8 && senha.chars().anyMatch(Character::isDigit)
                        && senha.chars().anyMatch(Character::isLetter)));
    }

    @Test
    void atualizarPerfilSincronizaEnderecoEEmailComContaSemPermitirMudarCpf() {
        UUID id = UUID.randomUUID();
        PacientesModel paciente = new PacientesModel();
        paciente.setId(id);
        paciente.setNome("Maria Antiga");
        paciente.setCpf("52998224725");
        paciente.setDataNascimento(LocalDate.of(1990, 5, 20));
        paciente.setEmail("maria@example.com");
        paciente.setCidade("Campinas");
        paciente.setEstado("SP");

        UsuariosModel conta = new UsuariosModel();
        conta.setId(77L);
        conta.setPacienteId(id);
        conta.setPerfil(Perfil.PACIENTE);
        conta.setCpf("52998224725");
        conta.setNome("Maria Antiga");
        conta.setEmail("maria@example.com");

        when(pacientesRepository.findById(id)).thenReturn(Optional.of(paciente));
        when(usuariosRepository.findByPacienteIdAndPerfil(id, Perfil.PACIENTE)).thenReturn(Optional.of(conta));
        when(usuariosRepository.findByEmailIgnoreCase("maria.nova@example.com")).thenReturn(Optional.empty());
        when(pacientesRepository.save(any(PacientesModel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuariosRepository.save(any(UsuariosModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PacienteAtualizacaoRequest request = new PacienteAtualizacaoRequest();
        request.setNome("Maria Nova");
        request.setDataNascimento(LocalDate.of(1990, 5, 20));
        request.setConvenio("SUS");
        request.setTelefone("11988887777");
        request.setEmail(" Maria.Nova@Example.com ");
        request.setCep("13010-000");
        request.setLogradouro("Rua Nova");
        request.setNumero("200");
        request.setComplemento("Apto 21");
        request.setBairro("Centro");
        request.setCidade("Campinas");
        request.setEstado("sp");

        PacientesModel atualizado = service.atualizar(id, request);

        assertEquals("Maria Nova", atualizado.getNome());
        assertEquals("52998224725", atualizado.getCpf());
        assertEquals("maria.nova@example.com", atualizado.getEmail());
        assertEquals("13010000", atualizado.getCep());
        assertEquals("Campinas", atualizado.getCidade());
        assertEquals("SP", atualizado.getEstado());
        assertEquals("maria.nova@example.com", conta.getEmail());
        assertEquals("Maria Nova", conta.getNome());
        verify(pacientesRepository).save(paciente);
        verify(usuariosRepository).save(conta);
    }

}
