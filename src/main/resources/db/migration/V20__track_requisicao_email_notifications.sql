ALTER TABLE requisicoes
    ADD COLUMN notificacao_status_enviado VARCHAR(20),
    ADD COLUMN notificacao_tentativa_em TIMESTAMP,
    ADD COLUMN notificacao_tentativas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN lembrete_pendente_enviado_em TIMESTAMP,
    ADD COLUMN lembrete_pendente_tentativa_em TIMESTAMP,
    ADD COLUMN lembrete_pendente_tentativas INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_requisicoes_notificacao_pendente
    ON requisicoes (status, notificacao_status_enviado, notificacao_tentativa_em, criado_em);

CREATE INDEX idx_requisicoes_lembrete_pendente
    ON requisicoes (status, criado_em, lembrete_pendente_enviado_em, lembrete_pendente_tentativa_em)
    WHERE status = 'PENDENTE';
