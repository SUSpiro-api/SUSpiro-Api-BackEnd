package com.app.suspiro.service;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.app.suspiro.exception.EnvioDeEmailException;
import com.app.suspiro.model.AgendamentosModel;
import com.app.suspiro.model.PacientesModel;
import com.app.suspiro.model.RequisicoesModel;
import com.app.suspiro.model.UsuariosModel;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final JavaMailSender mailSender;
    private final String remetente;
    private final String urlDoSistema;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${MAIL_FROM}") String remetente,
            @Value("${FRONTEND_URL}") String urlDoSistema) {

        this.mailSender = mailSender;
        this.remetente = remetente;
        this.urlDoSistema = urlDoSistema;
    }

    public void enviarPrimeiroAcesso(UsuariosModel usuario, String senha) {

        SimpleMailMessage mensagem = new SimpleMailMessage();

        mensagem.setFrom(remetente);
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject("SUSpiro - dados do seu primeiro acesso");
        mensagem.setText(montarCorpo(usuario, senha));

        try {

            mailSender.send(mensagem);

        } catch (MailException e) {

            log.error(
                    "Falha ao enviar o e-mail de primeiro acesso do usuário {}",
                    usuario.getCpf(), e
            );

            throw new EnvioDeEmailException(
                    "Não foi possível enviar o e-mail de primeiro acesso", e
            );
        }

        log.info(
                "E-mail de primeiro acesso enviado para o usuário {}",
                usuario.getCpf()
        );
    }

    private String montarCorpo(UsuariosModel usuario, String senha) {

        return """
                Olá, %s!

                Sua conta no SUSpiro foi criada.

                Endereço: %s
                CPF (login): %s
                Senha provisória: %s

                Esta senha foi gerada pelo sistema e serve apenas para o primeiro
                acesso: ao entrar, você será solicitado a cadastrar uma nova senha,
                e só depois disso o sistema será liberado.

                Este é um e-mail automático, não responda.
                """.formatted(
                usuario.getNome(),
                urlDoSistema,
                usuario.getCpf(),
                senha
        );
    }


    public void enviarAcompanhamento(RequisicoesModel requisicao) {

        PacientesModel paciente = requisicao.getPaciente();

        if (paciente.getEmail() == null || paciente.getEmail().isBlank()) {

            log.info(
                    "Paciente da requisição {} não possui e-mail, aviso não enviado",
                    requisicao.getProtocolo()
            );
            return;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();

        mensagem.setFrom(remetente);
        mensagem.setTo(paciente.getEmail());
        mensagem.setSubject(
                "SUSpiro - requisição " + requisicao.getProtocolo() + ": "
                        + tituloDaSituacao(requisicao)
        );
        mensagem.setText(montarCorpoAcompanhamento(requisicao));

        try {

            mailSender.send(mensagem);

        } catch (MailException e) {

            log.error(
                    "Falha ao enviar o e-mail de acompanhamento da requisição {}",
                    requisicao.getProtocolo(), e
            );
            return;
        }

        log.info(
                "E-mail de acompanhamento ({}) enviado para a requisição {}",
                requisicao.getStatus(), requisicao.getProtocolo()
        );
    }

    private String tituloDaSituacao(RequisicoesModel requisicao) {

        return switch (requisicao.getStatus()) {
            case PENDENTE -> "cadastrada";
            case AGENDADA -> "agendada";
            case CONCLUIDA -> "concluída";
            case VENCIDA -> "vencida";
        };
    }

    private String montarCorpoAcompanhamento(RequisicoesModel requisicao) {

        String detalhes = switch (requisicao.getStatus()) {

            case PENDENTE -> """
                    Sua requisição foi cadastrada e aguarda agendamento.
                    Ela precisa ser agendada até %s; depois dessa data ela vence.
                    """.formatted(requisicao.getDataVencimento().format(DATA));

            case AGENDADA -> {
                AgendamentosModel agendamento = requisicao.getAgendamento();

                yield """
                        Sua requisição foi agendada.

                        Data: %s
                        Local: %s
                        """.formatted(
                        agendamento.getDataHora().format(DATA_HORA),
                        agendamento.getLocal()
                );
            }

            case CONCLUIDA -> """
                    Sua requisição foi concluída.
                    """;

            case VENCIDA -> """
                    Sua requisição venceu em %s sem ter sido agendada.
                    Procure a sua unidade de saúde para orientação.
                    """.formatted(requisicao.getDataVencimento().format(DATA));
        };

        return """
                Olá, %s!

                Requisição: %s
                Protocolo: %s

                %s
                Guarde o número do protocolo: com ele e o seu CPF você pode
                acompanhar a situação da requisição a qualquer momento em %s

                Este é um e-mail automático, não responda.
                """.formatted(
                requisicao.getPaciente().getNome(),
                requisicao.getDescricao(),
                requisicao.getProtocolo(),
                detalhes,
                urlDoSistema
        );
    }
}
