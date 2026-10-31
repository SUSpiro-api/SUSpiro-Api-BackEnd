-- Incrementada a cada troca/redefinição de senha para invalidar JWTs anteriores.
ALTER TABLE usuarios
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN usuarios.token_version IS
    'Versão de sessão incluída no JWT; qualquer incremento revoga tokens emitidos com a versão anterior.';
