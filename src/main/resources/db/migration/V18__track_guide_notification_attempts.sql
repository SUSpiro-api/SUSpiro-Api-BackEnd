-- Registra as tentativas de entrega para limitar retries e evitar que falhas antigas
-- monopolizem os lotes de notificações. Os timestamps de envio existentes são preservados.
ALTER TABLE guias
    ADD COLUMN lembrete_pendente_tentativa_em TIMESTAMP NULL,
    ADD COLUMN lembrete_pendente_tentativas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN notificacao_agendamento_tentativa_em TIMESTAMP NULL,
    ADD COLUMN notificacao_agendamento_tentativas INTEGER NOT NULL DEFAULT 0;

ALTER TABLE guias
    ADD CONSTRAINT chk_guias_lembrete_tentativas CHECK (lembrete_pendente_tentativas >= 0),
    ADD CONSTRAINT chk_guias_confirmacao_tentativas CHECK (notificacao_agendamento_tentativas >= 0);

-- Índices parciais mantêm o job eficiente em tabelas de guias grandes.
CREATE INDEX idx_guias_pending_reminder_due
    ON guias (criado_em, lembrete_pendente_enviado_em, lembrete_pendente_tentativa_em, id)
    WHERE status = 'AGUARDANDO_AGENDAMENTO';

CREATE INDEX idx_guias_scheduled_confirmation_due
    ON guias (notificacao_agendamento_tentativa_em, atualizado_em, id)
    WHERE status = 'AGENDADO' AND notificacao_agendamento_enviada_em IS NULL;
