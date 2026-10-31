package com.app.suspiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.app.suspiro.enums.StatusGuia;
import com.app.suspiro.enums.TipoGuia;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.model.AgendamentosModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.RequisicoesModel;
import com.app.suspiro.enums.StatusRequisicao;
import com.app.suspiro.model.UsuariosModel;

class EmailServiceTest {
    @Test
    void senhaTemporariaDoPacienteIncluiCredencialLinkEOrientacaoDeTrocaObrigatoria() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "no-reply@example.com", "https://app.example.com/");
        UsuariosModel usuario = new UsuariosModel();
        usuario.setId(42L);
        usuario.setNome("Maria Exemplo");
        usuario.setCpf("52998224725");
        usuario.setEmail("maria@example.com");

        service.enviarSenhaTemporariaPaciente(usuario, "A1b!cD2eF3gH");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertEquals("maria@example.com", mensagem.getTo()[0]);
        assertTrue(mensagem.getText().contains("52998224725"));
        assertTrue(mensagem.getText().contains("A1b!cD2eF3gH"));
        assertTrue(mensagem.getText().contains("https://app.example.com/login"));
        assertTrue(mensagem.getText().contains("crie uma nova senha pessoal"));
    }

    @Test
    void confirmacaoDeAgendamentoIncluiDataHorarioProtocoloEClinicaSelecionada() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "no-reply@example.com", "https://app.example.com");
        GuiaModel guia = new GuiaModel();
        guia.setPacienteNome("Maria Exemplo");
        guia.setProtocolo("GUIA-TESTE-123");
        guia.setTipo(TipoGuia.CONSULTA);
        guia.setEspecialidade("Cardiologia");
        guia.setStatus(StatusGuia.AGENDADO);
        guia.setDataSolicitada(LocalDate.of(2026, 11, 5));
        guia.setDataAgendamento(LocalDate.of(2026, 11, 12));
        guia.setHoraAgendamento(LocalTime.of(9, 30));
        guia.setEstabelecimentoNome("Unidade de Saúde Central");
        guia.setEstabelecimentoEndereco("Rua das Palmeiras, 120");
        guia.setEstabelecimentoCidade("Cotia");
        guia.setEstabelecimentoEstado("SP");

        boolean enviado = service.enviarConfirmacaoAgendamentoGuia(guia, "maria@example.com");

        assertTrue(enviado);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("maria@example.com", captor.getValue().getTo()[0]);
        assertTrue(captor.getValue().getText().contains("GUIA-TESTE-123"));
        assertTrue(captor.getValue().getText().contains("12/11/2026 às 09:30"));
        assertTrue(captor.getValue().getText().contains("Unidade de Saúde Central"));
        assertTrue(captor.getValue().getText().contains("Rua das Palmeiras, 120"));
        assertTrue(captor.getValue().getText().contains("Cotia"));
        assertTrue(captor.getValue().getText().contains("SP"));
        assertTrue(captor.getValue().getText().contains("Endereço/local de atendimento: Rua das Palmeiras, 120"));
    }


    @Test
    void lembreteDeGuiaNaoAgendadaIncluiProtocoloTipoEDataDaSolicitacao() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "no-reply@example.com", "https://app.example.com");
        GuiaModel guia = new GuiaModel();
        guia.setPacienteNome("Maria Exemplo");
        guia.setProtocolo("GUIA-PENDENTE-17");
        guia.setTipo(TipoGuia.EXAME);
        guia.setEspecialidade("Ultrassonografia");
        guia.setDataSolicitada(LocalDate.of(2026, 10, 20));

        assertTrue(service.enviarLembreteGuiaNaoAgendada(guia, "maria@example.com"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertEquals("maria@example.com", mensagem.getTo()[0]);
        assertEquals("SUSpiro - lembrete: guia ainda não agendada", mensagem.getSubject());
        assertTrue(mensagem.getText().contains("GUIA-PENDENTE-17"));
        assertTrue(mensagem.getText().contains("exame"));
        assertTrue(mensagem.getText().contains("20/10/2026"));
    }

    @Test
    void confirmacaoDeRequisicaoIncluiUnidadeSelecionadaNoCampoDeEndereco() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "no-reply@example.com", "https://app.example.com");
        PacientesModel paciente = new PacientesModel();
        paciente.setNome("Lucas Exemplo");
        RequisicoesModel requisicao = new RequisicoesModel();
        requisicao.setProtocolo("REQ-TESTE-321");
        requisicao.setDescricao("Consulta cardiológica");
        requisicao.setPaciente(paciente);
        requisicao.setStatus(StatusRequisicao.AGENDADA);
        AgendamentosModel agendamento = new AgendamentosModel();
        agendamento.setDataHora(LocalDateTime.of(2026, 11, 12, 9, 30));
        agendamento.setLocal("Unidade de Saúde Regional");
        requisicao.setAgendamento(agendamento);

        assertTrue(service.enviarAcompanhamento(requisicao, "lucas@example.com"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("Unidade / endereço do atendimento: Unidade de Saúde Regional"));
        assertTrue(captor.getValue().getText().contains("12/11/2026 às 09:30"));
    }

}
