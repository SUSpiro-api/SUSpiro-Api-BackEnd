-- Guarda o estado das notificações de guias para permitir tentativas seguras após reinícios.
ALTER TABLE guias
    ADD COLUMN lembrete_pendente_enviado_em TIMESTAMP,
    ADD COLUMN notificacao_agendamento_enviada_em TIMESTAMP;

-- Não dispara confirmações retroativas para guias que já estavam agendadas antes desta funcionalidade.
UPDATE guias
SET notificacao_agendamento_enviada_em = atualizado_em
WHERE status = 'AGENDADO';

COMMENT ON COLUMN guias.lembrete_pendente_enviado_em IS
    'Data/hora do último lembrete enviado enquanto a guia aguardava agendamento.';
COMMENT ON COLUMN guias.notificacao_agendamento_enviada_em IS
    'Data/hora em que a confirmação do agendamento foi enviada ao paciente.';
