ALTER TABLE usuarios
    ADD COLUMN token_redefinicao_senha_hash VARCHAR(64),
    ADD COLUMN token_redefinicao_senha_expira_em TIMESTAMP WITHOUT TIME ZONE;

COMMENT ON COLUMN usuarios.token_redefinicao_senha_hash IS
    'Hash SHA-256 de token de recuperação de senha; o token original nunca é persistido.';
COMMENT ON COLUMN usuarios.token_redefinicao_senha_expira_em IS
    'Data/hora limite para uso único do link de recuperação de senha.';
