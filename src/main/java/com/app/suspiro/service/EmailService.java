package com.app.suspiro.service;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.app.suspiro.exception.EnvioDeEmailException;
import com.app.suspiro.model.AgendamentosModel;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.enums.TipoGuia;
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

    /** Envia a credencial inicial para o paciente; a API só persiste o hash da senha. */
    public void enviarSenhaTemporariaPaciente(UsuariosModel usuario, String senhaTemporaria) {
        exigirSmtpConfigurado();
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new EnvioDeEmailException("O paciente precisa de um e-mail válido para receber a senha temporária");
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject("SUSpiro - seus dados para o primeiro acesso");
        mensagem.setText("""
                Olá, %s!

                Sua conta SUSpiro foi criada pela equipe de atendimento.

                CPF de acesso: %s
                Senha temporária: %s
                Acesse o SUSpiro: %s/login

                Entre na plataforma usando seu CPF e a senha temporária. Por segurança, o sistema solicitará
                que você crie uma nova senha pessoal antes de acessar as informações de saúde.

                Não compartilhe esta senha. Se você não esperava este cadastro, entre em contato com a unidade
                de saúde responsável. Esta é uma mensagem automática; não responda a este e-mail.
                """.formatted(usuario.getNome(), usuario.getCpf(), senhaTemporaria, urlDoSistema.replaceAll("/+$", "")));

        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar credencial temporária de paciente (usuarioId={})", usuario.getId());
            throw new EnvioDeEmailException("Não foi possível enviar a senha temporária por e-mail", e);
        }
        log.info("Credencial temporária enviada para primeiro acesso (usuarioId={})", usuario.getId());
    }

    public void enviarLinkRedefinicao(UsuariosModel usuario, String token) {
        exigirSmtpConfigurado();
        String baseUrl = urlDoSistema.replaceAll("/+$", "");
        String link = baseUrl + "/recuperar-senha?token=" + token;

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject("SUSpiro - link para redefinir sua senha");
        mensagem.setText("""
                Olá, %s!

                Recebemos um pedido para redefinir a senha da sua conta SUSpiro.
                Acesse o link abaixo para criar uma nova senha:

                %s

                O link expira em 30 minutos e pode ser usado uma única vez.
                Se você não solicitou essa alteração, ignore este e-mail.

                Por segurança, nunca compartilhe este link.
                """.formatted(usuario.getNome(), link));

        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar link de recuperação de senha (usuarioId={})", usuario.getId());
            throw new EnvioDeEmailException("Não foi possível enviar o link de recuperação", e);
        }

        log.info("Link de recuperação de senha enviado (usuarioId={})", usuario.getId());
    }

    public void enviarAcompanhamento(RequisicoesModel requisicao) {
        enviarAcompanhamento(requisicao, requisicao.getPaciente() == null ? null : requisicao.getPaciente().getEmail());
    }

    /** Retorna true apenas quando o servidor SMTP aceitou a mensagem. */
    public boolean enviarAcompanhamento(RequisicoesModel requisicao, String emailDestino) {
        if (!smtpConfigurado()) return false;
        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("Requisição {} sem e-mail de destino; notificação será tentada novamente", requisicao.getProtocolo());
            return false;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(emailDestino);
        mensagem.setSubject("SUSpiro - requisição " + requisicao.getProtocolo() + ": " + tituloDaSituacao(requisicao));
        mensagem.setText(montarCorpoAcompanhamento(requisicao));

        try {
            mailSender.send(mensagem);
            log.info("E-mail de acompanhamento aceito pelo SMTP (status={}, protocolo={})", requisicao.getStatus(), requisicao.getProtocolo());
            return true;
        } catch (MailException e) {
            log.error("Falha ao enviar acompanhamento (status={}, protocolo={})", requisicao.getStatus(), requisicao.getProtocolo(), e);
            return false;
        }
    }

    public boolean enviarLembreteRequisicaoPendente(RequisicoesModel requisicao, String emailDestino) {
        if (!smtpConfigurado()) return false;
        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("Requisição {} pendente sem e-mail de destino", requisicao.getProtocolo());
            return false;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(emailDestino);
        mensagem.setSubject("SUSpiro - lembrete: solicitação ainda não agendada");
        mensagem.setText(corpoLembreteRequisicao(requisicao));
        try {
            mailSender.send(mensagem);
            log.info("Lembrete de requisição aceito pelo SMTP (requisicaoId={})", requisicao.getId());
            return true;
        } catch (MailException e) {
            log.error("Falha ao enviar lembrete de requisição (requisicaoId={})", requisicao.getId(), e);
            return false;
        }
    }

    private String corpoLembreteRequisicao(RequisicoesModel requisicao) {
        return """
                Olá, %s!

                Este é um lembrete de que sua solicitação ainda aguarda agendamento pela unidade de saúde.

                Solicitação: %s
                Protocolo: %s
                Data-limite registrada: %s

                Não é necessário abrir outra solicitação para o mesmo atendimento. Acompanhe a situação pelo SUSpiro: %s

                O aviso automático é repetido a cada cinco dias enquanto a solicitação permanecer pendente.
                Esta é uma mensagem automática; não responda.
                """.formatted(requisicao.getPaciente().getNome(), requisicao.getDescricao(), requisicao.getProtocolo(),
                requisicao.getDataVencimento().format(DATA), urlDoSistema.replaceAll("/+$", ""));
    }


    /** Envia a confirmação quando a recepção agenda uma guia. Retorna false se não foi possível entregar. */
    public boolean enviarConfirmacaoAgendamentoGuia(GuiaModel guia, String emailDestino) {
        if (!smtpConfigurado()) return false;
        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("Guia {} agendada, mas o paciente não possui e-mail cadastrado", guia.getId());
            return false;
        }
        if (guia.getDataAgendamento() == null || guia.getHoraAgendamento() == null) {
            log.warn("Guia {} está marcada como agendada, mas não possui data e horário válidos", guia.getId());
            return false;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(emailDestino);
        mensagem.setSubject("SUSpiro - " + tituloTipoGuia(guia) + " agendada");
        mensagem.setText(montarCorpoConfirmacaoGuia(guia));

        try {
            mailSender.send(mensagem);
            log.info("Confirmação de agendamento enviada (guiaId={})", guia.getId());
            return true;
        } catch (MailException e) {
            log.error("Falha ao enviar confirmação de agendamento (guiaId={})", guia.getId(), e);
            return false;
        }
    }

    /** Envia um lembrete quando a guia continua aguardando agendamento há cinco dias. */
    public boolean enviarLembreteGuiaNaoAgendada(GuiaModel guia, String emailDestino) {
        if (!smtpConfigurado()) return false;
        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("Guia {} pendente sem e-mail de destino", guia.getId());
            return false;
        }
        if (guia.getDataSolicitada() == null) {
            log.warn("Guia {} pendente sem data de solicitação; lembrete não enviado", guia.getId());
            return false;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(emailDestino);
        mensagem.setSubject("SUSpiro - lembrete: guia ainda não agendada");
        mensagem.setText(montarCorpoLembreteGuia(guia));

        try {
            mailSender.send(mensagem);
            log.info("Lembrete de guia aceito pelo SMTP (guiaId={})", guia.getId());
            return true;
        } catch (MailException e) {
            log.error("Falha ao enviar lembrete de guia (guiaId={})", guia.getId(), e);
            return false;
        }
    }

    private String montarCorpoConfirmacaoGuia(GuiaModel guia) {
        String detalhes = detalhesGuia(guia);
        String dataHora = guia.getDataAgendamento().atTime(guia.getHoraAgendamento()).format(DATA_HORA);
        return """
                Olá, %s!

                Sua solicitação no SUSpiro foi agendada com sucesso.

                Protocolo: %s
                Tipo: %s
                %sData e horário: %s
                Unidade: %s
                Endereço/local de atendimento: %s

                Acesse o SUSpiro para consultar as informações da sua guia: %s
                Caso tenha dúvidas, entre em contato com a unidade de saúde responsável.

                Este é um e-mail automático, não responda.
                """.formatted(guia.getPacienteNome(), guia.getProtocolo(), tituloTipoGuia(guia), detalhes, dataHora,
                valorOuPadrao(guia.getEstabelecimentoNome(), "Unidade informada no agendamento"),
                enderecoUnidade(guia), urlDoSistema.replaceAll("/+$", ""));
    }

    private String montarCorpoLembreteGuia(GuiaModel guia) {
        return """
                Olá, %s!

                Este é um lembrete de que a sua solicitação ainda aguarda agendamento pela unidade de saúde.

                Protocolo: %s
                Tipo: %s
                %sData da solicitação: %s

                Até o momento, a consulta, exame ou procedimento ainda não foi agendado. Não é necessário abrir uma nova solicitação para a mesma guia. Se precisar de orientação, entre em contato com a unidade de saúde responsável.

                Acompanhe a solicitação no SUSpiro: %s

                Este aviso é automático e será repetido a cada 5 dias enquanto a guia continuar aguardando agendamento.
                Este é um e-mail automático, não responda.
                """.formatted(
                guia.getPacienteNome(),
                guia.getProtocolo(),
                tituloTipoGuia(guia),
                detalhesGuia(guia),
                guia.getDataSolicitada().format(DATA),
                urlDoSistema.replaceAll("/+$", ""));
    }

    private String enderecoUnidade(GuiaModel guia) {
        String cidadeEstado = guia.getEstabelecimentoCidade() == null ? "" : guia.getEstabelecimentoCidade()
                + (guia.getEstabelecimentoEstado() == null ? "" : "/" + guia.getEstabelecimentoEstado());
        String endereco = valorOuPadrao(guia.getEstabelecimentoEndereco(), "");
        if (endereco.isBlank()) {
            if (!cidadeEstado.isBlank()) return cidadeEstado;
            return valorOuPadrao(guia.getEstabelecimentoNome(), "Unidade selecionada no agendamento");
        }
        return cidadeEstado.isBlank() ? endereco : endereco + " · " + cidadeEstado;
    }

    private boolean smtpConfigurado() {
        if (remetente == null || remetente.isBlank()) {
            log.error("E-mail não enviado: MAIL_FROM está vazio. Configure MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD e MAIL_FROM no .env do backend.");
            return false;
        }
        // O bean padrão do Spring Boot é JavaMailSenderImpl; checa credenciais antes de tentar enviar.
        // Mocks e implementações alternativas continuam podendo ser usados em testes.
        if (mailSender instanceof JavaMailSenderImpl smtp
                && ((smtp.getUsername() == null || smtp.getUsername().isBlank())
                || (smtp.getPassword() == null || smtp.getPassword().isBlank()))) {
            log.error("E-mail não enviado: MAIL_USERNAME ou MAIL_PASSWORD está vazio. Para Gmail, use uma Senha de app.");
            return false;
        }
        return true;
    }

    private void exigirSmtpConfigurado() {
        if (!smtpConfigurado()) {
            throw new EnvioDeEmailException("Serviço de e-mail não configurado. Preencha MAIL_USERNAME, MAIL_PASSWORD e MAIL_FROM no .env do backend; para Gmail, use uma Senha de app.");
        }
    }

    private String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    private String detalhesGuia(GuiaModel guia) {
        StringBuilder detalhes = new StringBuilder();
        if (guia.getEspecialidade() != null && !guia.getEspecialidade().isBlank()) {
            detalhes.append("Especialidade: ").append(guia.getEspecialidade()).append("\n");
        }
        if (guia.getDetalhe() != null && !guia.getDetalhe().isBlank()) {
            detalhes.append("Detalhes: ").append(guia.getDetalhe()).append("\n");
        }
        return detalhes.toString();
    }

    private String tituloTipoGuia(GuiaModel guia) {
        TipoGuia tipo = guia.getTipo();
        return switch (tipo) {
            case CONSULTA -> "consulta";
            case RETORNO -> "retorno";
            case EXAME -> "exame";
            case ENCAMINHAMENTO -> "encaminhamento";
        };
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

                        Data e horário: %s
                        Unidade / endereço do atendimento: %s
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
