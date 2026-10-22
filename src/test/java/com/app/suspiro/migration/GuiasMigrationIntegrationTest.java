package com.app.suspiro.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.app.suspiro.IntegracaoTestBase;

class GuiasMigrationIntegrationTest extends IntegracaoTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void chaveEstrangeiraDeGuiasParaPacientesExisteEEstaValidada() {
        Map<String, Object> fk = jdbc.queryForMap("""
                SELECT referenciada.relname AS tabela_referenciada, c.convalidated AS validada
                FROM pg_constraint c
                JOIN pg_class referenciada ON referenciada.oid = c.confrelid
                WHERE c.conname = 'fk_guias_paciente' AND c.contype = 'f'
                """);

        assertThat(fk).containsEntry("tabela_referenciada", "pacientes");
        assertThat(fk).containsEntry("validada", true);
    }

    @Test
    void bancoRecusaGuiaComPacienteInexistente() {
        Long solicitante = jdbc.queryForObject("SELECT id FROM usuarios WHERE perfil = 'SUPER_ADMIN'", Long.class);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO guias (paciente_id, paciente_nome, paciente_cpf, tipo, data_solicitada, prioridade,
                                   status, solicitante_id, solicitante_nome)
                VALUES (?, 'Ninguém', '11144477735', 'CONSULTA', CURRENT_DATE, 'NORMAL',
                        'AGUARDANDO_AGENDAMENTO', ?, 'Profissional')
                """, UUID.randomUUID(), solicitante))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_guias_paciente");
    }
}
