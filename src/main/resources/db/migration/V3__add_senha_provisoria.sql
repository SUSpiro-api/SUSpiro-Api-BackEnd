ALTER TABLE usuarios
    ADD COLUMN senha_provisoria BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN usuarios.senha_provisoria IS
    'Senha gerada pelo sistema, ainda nao trocada pelo usuario. Enquanto for TRUE, o acesso fica limitado a troca de senha.';
