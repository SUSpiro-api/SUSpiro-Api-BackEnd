package com.app.suspiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.app.suspiro.exception.EnvioDeEmailException;
import com.app.suspiro.model.UsuariosModel;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

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
                Senha: %s

                Esta senha foi gerada pelo sistema. Guarde-a em local seguro e
                não a compartilhe com ninguém.

                Este é um e-mail automático, não responda.
                """.formatted(
                usuario.getNome(),
                urlDoSistema,
                usuario.getCpf(),
                senha
        );
    }
}
