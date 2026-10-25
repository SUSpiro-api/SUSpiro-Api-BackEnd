package com.app.suspiro.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.suspiro.dto.TrocaDeSenhaRequest;
import com.app.suspiro.dto.RedefinirSenhaRequest;
import com.app.suspiro.dto.UsuarioCadastroRequest;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.exception.EnvioDeEmailException;
import com.app.suspiro.exception.RegraDeNegocioException;
import com.app.suspiro.exception.DadosInvalidosException;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;

class UsuarioServiceTest {

    private UsuariosRepository repository;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private UsuarioService service;

    @BeforeEach
    void setUp() {

        repository = Mockito.mock(UsuariosRepository.class);
        emailService = Mockito.mock(EmailService.class);
        passwordEncoder = new BCryptPasswordEncoder();

        service = new UsuarioService(repository, passwordEncoder, emailService);

        when(repository.save(any(UsuariosModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UsuarioCadastroRequest request(Perfil perfil) {

        UsuarioCadastroRequest request = new UsuarioCadastroRequest();

        request.setCpf("52998224725");
        request.setNome("Fulano de Tal");
        request.setEmail("Fulano@Exemplo.com");
        request.setPerfil(perfil);
        request.setSenhaTemporaria("SenhaTemporaria#2026");

        return request;
    }


    @Test
    void superAdminCadastraGerente() {

        UsuariosModel salvo =
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN);

        assertEquals(Perfil.GERENTE, salvo.getPerfil());
        assertEquals("fulano@exemplo.com", salvo.getEmail());
        verify(repository).save(any(UsuariosModel.class));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"SUPER_ADMIN", "PACIENTE"})
    void superAdminNaoPodeCriarSuperAdminNemPaciente(Perfil perfil) {

        assertThrows(AccessDeniedException.class, () ->
                service.cadastrar(request(perfil), Perfil.SUPER_ADMIN));

        verify(repository, never()).save(any(UsuariosModel.class));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void superAdminCadastraProfissionaisPrevistos(Perfil perfil) {
        UsuariosModel salvo = service.cadastrar(request(perfil), Perfil.SUPER_ADMIN);
        assertEquals(perfil, salvo.getPerfil());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void gerenteCadastraFuncionarios(Perfil perfil) {

        UsuariosModel salvo =
                service.cadastrar(request(perfil), Perfil.GERENTE);

        assertEquals(perfil, salvo.getPerfil());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"SUPER_ADMIN", "GERENTE"})
    void gerenteNaoCadastraGerenteNemSuperAdmin(Perfil perfil) {

        assertThrows(AccessDeniedException.class, () ->
                service.cadastrar(request(perfil), Perfil.GERENTE));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void demaisPerfisNaoCadastramNinguem(Perfil perfilDoCriador) {

        assertThrows(AccessDeniedException.class, () ->
                service.cadastrar(request(Perfil.ATENDENTE), perfilDoCriador));
    }

    @Test
    void senhaTemporariaInformadaEhArmazenadaCriptografada() {
        UsuariosModel salvo = service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN);
        assertTrue(passwordEncoder.matches("SenhaTemporaria#2026", salvo.getSenha()));
        assertFalse(salvo.getSenha().equals("SenhaTemporaria#2026"));
    }

    @Test
    void senhaTemporariaEhObrigatoriaNaRequisicao() {
        UsuarioCadastroRequest request = request(Perfil.GERENTE);
        request.setSenhaTemporaria(" ");
        // O serviço também valida a senha para que chamadas internas não contornem as regras do DTO.
        assertThrows(DadosInvalidosException.class, () ->
                service.cadastrar(request, Perfil.SUPER_ADMIN));
    }

    @Test
    void usuarioNasceComSenhaProvisoria() {

        UsuariosModel salvo =
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN);

        assertTrue(salvo.isSenhaProvisoria());
    }

    @Test
    void cpfDuplicadoEhRejeitado() {

        when(repository.existsByCpf(anyString())).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () ->
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN));
    }

    @Test
    void emailDuplicadoEhRejeitado() {

        when(repository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () ->
                service.cadastrar(request(Perfil.GERENTE), Perfil.SUPER_ADMIN));
    }



    private UsuariosModel usuarioComSenha(String senha) {

        UsuariosModel usuario = new UsuariosModel();

        usuario.setCpf("52998224725");
        usuario.setNome("Fulano de Tal");
        usuario.setEmail("fulano@exemplo.com");
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(Perfil.GERENTE);
        usuario.setAtivo(true);
        usuario.setSenhaProvisoria(true);

        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(usuario));

        return usuario;
    }

    private TrocaDeSenhaRequest trocaDeSenha(String atual, String nova) {

        TrocaDeSenhaRequest request = new TrocaDeSenhaRequest();

        request.setSenhaAtual(atual);
        request.setNovaSenha(nova);

        return request;
    }


    @Test
    void logoutIncrementaVersaoParaRevogarJwtsAnteriores() {
        UsuariosModel usuario = usuarioComSenha("SenhaAntiga1!");

        service.revogarSessoesAtivas("52998224725");

        assertEquals(1L, usuario.getTokenVersion());
        verify(repository).save(usuario);
    }

    @Test
    void trocaDeSenhaGravaONovoHashELimpaAFlagProvisoria() {

        UsuariosModel usuario = usuarioComSenha("SenhaAntiga1!");

        service.trocarSenha(
                "52998224725",
                trocaDeSenha("SenhaAntiga1!", "SenhaNova22@")
        );

        assertTrue(passwordEncoder.matches("SenhaNova22@", usuario.getSenha()));
        assertFalse(usuario.isSenhaProvisoria());
        assertEquals(1L, usuario.getTokenVersion());
        verify(repository).save(usuario);
    }

    @Test
    void trocaDeSenhaExigeASenhaAtualCorreta() {

        usuarioComSenha("SenhaAntiga1!");

        assertThrows(RegraDeNegocioException.class, () ->
                service.trocarSenha(
                        "52998224725",
                        trocaDeSenha("SenhaErrada1!", "SenhaNova22@")
                ));

        verify(repository, never()).save(any(UsuariosModel.class));
    }

    @Test
    void novaSenhaNaoPodeSerIgualAAtual() {

        usuarioComSenha("SenhaAntiga1!");

        assertThrows(RegraDeNegocioException.class, () ->
                service.trocarSenha(
                        "52998224725",
                        trocaDeSenha("SenhaAntiga1!", "SenhaAntiga1!")
                ));

        verify(repository, never()).save(any(UsuariosModel.class));
    }


    @Test
    void solicitaLinkDeRecuperacaoSemEnviarSenhaEGuardaSomenteHashDoToken() {
        UsuariosModel usuario = usuarioComSenha("SenhaAntiga1!");
        usuario.setEmail("fulano@exemplo.com");

        service.redefinirSenha("529.982.247-25");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarLinkRedefinicao(eq(usuario), tokenCaptor.capture());
        String tokenOriginal = tokenCaptor.getValue();
        assertTrue(tokenOriginal.length() >= 40);
        assertTrue(usuario.getTokenRedefinicaoSenhaHash() != null);
        org.junit.jupiter.api.Assertions.assertNotEquals(tokenOriginal, usuario.getTokenRedefinicaoSenhaHash());
        assertTrue(usuario.getTokenRedefinicaoSenhaExpiraEm().isAfter(LocalDateTime.now()));
    }

    @Test
    void redefinicaoPorLinkAlteraSenhaEInvalidaToken() {
        UsuariosModel usuario = usuarioComSenha("SenhaAntiga1!");
        usuario.setEmail("fulano@exemplo.com");
        service.redefinirSenha("52998224725");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarLinkRedefinicao(eq(usuario), tokenCaptor.capture());
        when(repository.findByTokenRedefinicaoSenhaHashAndTokenRedefinicaoSenhaExpiraEmAfter(
                anyString(), any(LocalDateTime.class))).thenReturn(Optional.of(usuario));

        service.confirmarRedefinicaoSenha(new RedefinirSenhaRequest(tokenCaptor.getValue(), "NovaSenhaForte2026!"));

        assertTrue(passwordEncoder.matches("NovaSenhaForte2026!", usuario.getSenha()));
        assertFalse(usuario.isSenhaProvisoria());
        org.junit.jupiter.api.Assertions.assertNull(usuario.getTokenRedefinicaoSenhaHash());
        org.junit.jupiter.api.Assertions.assertNull(usuario.getTokenRedefinicaoSenhaExpiraEm());
        verify(repository, Mockito.atLeast(2)).save(usuario);
    }

    private Collection<Perfil> perfisConsultados(Pageable paginacao) {

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Perfil>> perfis =
                ArgumentCaptor.forClass(Collection.class);

        verify(repository).findByPerfilIn(perfis.capture(), eq(paginacao));

        return perfis.getValue();
    }

    @Test
    void gerenteEnxergaApenasOsFuncionariosQueGerencia() {

        Pageable paginacao = PageRequest.of(0, 20);

        when(repository.findByPerfilIn(any(), eq(paginacao)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar(Perfil.GERENTE, paginacao);

        assertEquals(
                Set.of(Perfil.MEDICO, Perfil.ENFERMEIRO, Perfil.ATENDENTE),
                Set.copyOf(perfisConsultados(paginacao))
        );
    }

    @Test
    void superAdminEnxergaOsPerfisQuePodeGerenciar() {

        Pageable paginacao = PageRequest.of(0, 20);

        when(repository.findByPerfilIn(any(), eq(paginacao)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar(Perfil.SUPER_ADMIN, paginacao);

        assertEquals(
                Set.of(Perfil.GERENTE, Perfil.MEDICO, Perfil.ENFERMEIRO, Perfil.ATENDENTE),
                Set.copyOf(perfisConsultados(paginacao))
        );
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class,
            names = {"MEDICO", "ENFERMEIRO", "ATENDENTE"})
    void demaisPerfisNaoEnxergamNinguem(Perfil perfil) {

        Page<UsuariosModel> resultado =
                service.listar(perfil, PageRequest.of(0, 20));

        assertTrue(resultado.isEmpty());
        verify(repository, never()).findByPerfilIn(any(), any());
    }

    @Test
    void redefinirSenhaDeCpfInexistenteNaoRevelaSeCpfExiste() {
        when(repository.findByCpf("00000000000")).thenReturn(Optional.empty());

        service.redefinirSenha("00000000000");

        verify(repository, never()).save(any());
        verify(emailService, never()).enviarLinkRedefinicao(any(), anyString());
    }

    @Test
    void redefinirSenhaIgnoraUsuarioInativoOuSemEmail() {
        UsuariosModel usuario = usuarioComSenha("senha-antiga");
        usuario.setAtivo(false);

        service.redefinirSenha("52998224725");

        usuario.setAtivo(true);
        usuario.setEmail(null);
        service.redefinirSenha("52998224725");

        verify(repository, never()).save(any());
        verify(emailService, never()).enviarLinkRedefinicao(any(), anyString());
    }

    @Test
    void falhaDeEmailRemoveTokenDeRecuperacaoSemRevelarConta() {
        UsuariosModel usuario = usuarioComSenha("senha-antiga");
        Mockito.doThrow(new EnvioDeEmailException("falhou", null))
                .when(emailService).enviarLinkRedefinicao(any(), anyString());

        service.redefinirSenha("52998224725");

        org.junit.jupiter.api.Assertions.assertNull(usuario.getTokenRedefinicaoSenhaHash());
        org.junit.jupiter.api.Assertions.assertNull(usuario.getTokenRedefinicaoSenhaExpiraEm());
        verify(repository, Mockito.atLeast(2)).save(usuario);
    }

}
