ALTER TABLE usuarios
    ADD COLUMN tentativas_login_falhas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_ate TIMESTAMP NULL,
    ADD CONSTRAINT chk_usuarios_tentativas_login_falhas
        CHECK (tentativas_login_falhas >= 0);

COMMENT ON COLUMN usuarios.tentativas_login_falhas IS
    'Contador de senhas incorretas usado pela proteção de bloqueio temporário no login.';
COMMENT ON COLUMN usuarios.bloqueado_ate IS
    'Instante UTC de expiração do bloqueio temporário de autenticação; NULL significa desbloqueado.';
