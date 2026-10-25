package com.app.suspiro.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.app.suspiro.IntegracaoTestBase;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.security.SecurityAuth;

class GuiaControllerIntegrationTest extends IntegracaoTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UsuariosRepository usuariosRepository;

    private long medicoA;
    private long medicoB;
    private long atendente;
    private UUID pacienteMaria;
    private UUID pacienteJoao;

    @BeforeEach
    void prepararBanco() {
        jdbc.update("DELETE FROM guias");
        jdbc.update("DELETE FROM agendamentos");
        jdbc.update("DELETE FROM requisicoes");
        jdbc.update("DELETE FROM pacientes");
        jdbc.update("DELETE FROM usuarios WHERE perfil <> 'SUPER_ADMIN'");

        medicoA = usuario("10000000001", "Dra. Ana", Perfil.MEDICO);
        medicoB = usuario("10000000002", "Dr. Bruno", Perfil.MEDICO);
        atendente = usuario("10000000003", "Carla Atendente", Perfil.ATENDENTE);
        pacienteMaria = paciente("Maria Oficial", "11144477735", "700000000000001");
        pacienteJoao = paciente("João Oficial", "52998224725", "700000000000002");
    }

    // ---- POST /guias ----

    @Test
    void cadastroUsaDadosOficiaisDoPacienteMesmoComNomeECpfManipulados() throws Exception {
        mockMvc.perform(post("/guias").with(logado(medicoA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoGuia(pacienteMaria, "\"pacienteNome\":\"Nome Falso\",\"pacienteCpf\":\"12345678909\",")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteId").value(pacienteMaria.toString()))
                .andExpect(jsonPath("$.pacienteNome").value("Maria Oficial"))
                .andExpect(jsonPath("$.pacienteCpf").value("11144477735"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_AGENDAMENTO"));

        Map<String, Object> persistida = jdbc.queryForMap("SELECT paciente_nome, paciente_cpf, status FROM guias");
        assertThat(persistida)
                .containsEntry("paciente_nome", "Maria Oficial")
                .containsEntry("paciente_cpf", "11144477735")
                .containsEntry("status", "AGUARDANDO_AGENDAMENTO");
    }


    @Test
    void superAdminPodeEmitirGuia() throws Exception {
        Long superAdminId = jdbc.queryForObject("SELECT id FROM usuarios WHERE perfil = 'SUPER_ADMIN'", Long.class);
        mockMvc.perform(post("/guias").with(logado(superAdminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoGuia(pacienteMaria, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteId").value(pacienteMaria.toString()))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_AGENDAMENTO"));
    }

    @Test
    void cadastroSemNomeECpfNoCorpoContinuaFuncionando() throws Exception {
        mockMvc.perform(post("/guias").with(logado(medicoA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoGuia(pacienteMaria, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("Maria Oficial"));
    }

    @Test
    void cadastroComPacienteInexistenteRetorna404ENaoPersiste() throws Exception {
        mockMvc.perform(post("/guias").with(logado(medicoA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoGuia(UUID.randomUUID(), "\"pacienteNome\":\"Fantasma\",")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Paciente não encontrado."));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guias", Long.class)).isZero();
    }

    // ---- GET /guias/paciente/{pacienteId} ----

    @Test
    void historicoTrazGuiasDeTodosOsProfissionaisOrdenadasDaMaisRecente() throws Exception {
        LocalDateTime janeiro = LocalDateTime.of(2026, 1, 10, 9, 0);
        LocalDateTime marco = LocalDateTime.of(2026, 3, 5, 14, 30);

        long antiga = guia(pacienteMaria, medicoA, "Dra. Ana", janeiro, "AGENDADO");
        long empateMenorId = guia(pacienteMaria, medicoB, "Dr. Bruno", marco, "AGUARDANDO_AGENDAMENTO");
        long empateMaiorId = guia(pacienteMaria, medicoA, "Dra. Ana", marco, "AGUARDANDO_AGENDAMENTO");
        guia(pacienteJoao, medicoA, "Dra. Ana", marco, "AGUARDANDO_AGENDAMENTO");

        mockMvc.perform(get("/guias/paciente/{id}", pacienteMaria).with(logado(medicoB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].id").value(contains(
                        (int) empateMaiorId, (int) empateMenorId, (int) antiga)))
                .andExpect(jsonPath("$[*].solicitanteNome").value(containsInAnyOrder("Dra. Ana", "Dr. Bruno", "Dra. Ana")))
                .andExpect(jsonPath("$[*].pacienteId").value(contains(
                        pacienteMaria.toString(), pacienteMaria.toString(), pacienteMaria.toString())));
    }

    @Test
    void historicoSegueOContratoDeGuiaResponse() throws Exception {
        guia(pacienteMaria, medicoA, "Dra. Ana", LocalDateTime.of(2026, 2, 1, 8, 0), "AGUARDANDO_AGENDAMENTO");

        var resultado = mockMvc.perform(get("/guias/paciente/{id}", pacienteMaria).with(logado(medicoA)))
                .andExpect(status().isOk());

        for (String campo : new String[] {
                "id", "pacienteId", "pacienteNome", "pacienteCpf", "tipo", "especialidade", "detalhe",
                "dataSolicitada", "observacao", "prioridade", "status", "solicitanteId", "solicitanteNome",
                "dataAgendamento", "horaAgendamento", "agendadoPorId", "agendadoPorNome", "criadoEm" }) {
            resultado.andExpect(jsonPath("$[0]." + campo).hasJsonPath());
        }
    }

    @Test
    void historicoDePacienteSemGuiasRetornaListaVazia() throws Exception {
        mockMvc.perform(get("/guias/paciente/{id}", pacienteJoao).with(logado(medicoA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void historicoDePacienteInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/guias/paciente/{id}", UUID.randomUUID()).with(logado(medicoA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Paciente não encontrado."));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = { "SUPER_ADMIN", "GERENTE", "MEDICO", "ENFERMEIRO" })
    void historicoLiberadoParaPerfisClinicosEDeGestao(Perfil perfil) throws Exception {
        mockMvc.perform(get("/guias/paciente/{id}", pacienteMaria).with(logado(perfil)))
                .andExpect(status().isOk());
    }

    @Test
    void historicoNegadoParaAtendente() throws Exception {
        guia(pacienteMaria, medicoA, "Dra. Ana", LocalDateTime.of(2026, 2, 1, 8, 0), "AGUARDANDO_AGENDAMENTO");

        mockMvc.perform(get("/guias/paciente/{id}", pacienteMaria).with(logado(atendente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.pacienteNome").doesNotExist());
    }

    @Test
    void historicoSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/guias/paciente/{id}", pacienteMaria))
                .andExpect(status().isUnauthorized());
    }

    // ---- Regressão ----

    @Test
    void minhasListaApenasAsGuiasDoSolicitante() throws Exception {
        long daAna = guia(pacienteMaria, medicoA, "Dra. Ana", LocalDateTime.of(2026, 2, 1, 8, 0), "AGUARDANDO_AGENDAMENTO");
        guia(pacienteMaria, medicoB, "Dr. Bruno", LocalDateTime.of(2026, 2, 2, 8, 0), "AGUARDANDO_AGENDAMENTO");

        mockMvc.perform(get("/guias/minhas").with(logado(medicoA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(daAna));
    }

    @Test
    void pendentesListaSoAsGuiasAguardandoAgendamento() throws Exception {
        long pendente = guia(pacienteMaria, medicoA, "Dra. Ana", LocalDateTime.of(2026, 2, 1, 8, 0), "AGUARDANDO_AGENDAMENTO");
        guia(pacienteJoao, medicoA, "Dra. Ana", LocalDateTime.of(2026, 2, 2, 8, 0), "AGENDADO");

        mockMvc.perform(get("/guias/pendentes").with(logado(atendente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(pendente));
    }

    @Test
    void agendamentoFeitoPeloAtendenteENegadoParaMedico() throws Exception {
        long guia = guia(pacienteMaria, medicoA, "Dra. Ana", LocalDateTime.of(2026, 2, 1, 8, 0), "AGUARDANDO_AGENDAMENTO");
        String corpo = "{\"dataAgendamento\":\"" + LocalDate.now().plusDays(3) + "\",\"horaAgendamento\":\"10:30\"}";

        mockMvc.perform(patch("/guias/{id}/agendamento", guia).with(logado(medicoA))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/guias/{id}/agendamento", guia).with(logado(atendente))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.agendadoPorNome").value("Carla Atendente"));
    }

    // ---- Apoio ----

    private RequestPostProcessor logado(long usuarioId) {
        return user(new SecurityAuth(usuariosRepository.findById(usuarioId).orElseThrow()));
    }

    private RequestPostProcessor logado(Perfil perfil) {
        long id = perfil == Perfil.SUPER_ADMIN
                ? jdbc.queryForObject("SELECT id FROM usuarios WHERE perfil = 'SUPER_ADMIN'", Long.class)
                : usuario("2000000000" + perfil.ordinal(), "Usuário " + perfil, perfil);
        return logado(id);
    }

    private String corpoGuia(UUID pacienteId, String camposExtras) {
        return "{\"pacienteId\":\"" + pacienteId + "\"," + camposExtras
                + "\"tipo\":\"CONSULTA\",\"especialidade\":\"Cardiologia\","
                + "\"dataSolicitada\":\"" + LocalDate.now().plusDays(7) + "\",\"prioridade\":\"NORMAL\"}";
    }

    private long usuario(String cpf, String nome, Perfil perfil) {
        return jdbc.queryForObject(
                "INSERT INTO usuarios (cpf, nome, senha, perfil, senha_provisoria) VALUES (?, ?, 'x', ?, false) RETURNING id",
                Long.class, cpf, nome, perfil.name());
    }

    private UUID paciente(String nome, String cpf, String numeroSus) {
        return jdbc.queryForObject(
                "INSERT INTO pacientes (nome, cpf, data_nascimento, numero_sus) VALUES (?, ?, '1980-01-01', ?) RETURNING id",
                UUID.class, nome, cpf, numeroSus);
    }

    private long guia(UUID pacienteId, long solicitanteId, String solicitanteNome, LocalDateTime criadoEm, String status) {
        return jdbc.queryForObject("""
                INSERT INTO guias (paciente_id, paciente_nome, paciente_cpf, tipo, data_solicitada, prioridade,
                                   status, solicitante_id, solicitante_nome, criado_em, atualizado_em)
                SELECT p.id, p.nome, p.cpf, 'CONSULTA', CURRENT_DATE + 7, 'NORMAL', ?, ?, ?, ?, ?
                FROM pacientes p WHERE p.id = ?
                RETURNING id
                """, Long.class, status, solicitanteId, solicitanteNome, criadoEm, criadoEm, pacienteId);
    }
}
