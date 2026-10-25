-- Liga a conta de acesso ao registro persistente do paciente por UUID.
-- Contas legadas são associadas somente quando CPF e perfil coincidem.
ALTER TABLE usuarios ADD COLUMN paciente_id UUID;

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_paciente
    FOREIGN KEY (paciente_id) REFERENCES pacientes(id);

UPDATE usuarios u
SET paciente_id = p.id
FROM pacientes p
WHERE u.perfil = 'PACIENTE'
  AND u.cpf = p.cpf
  AND u.paciente_id IS NULL;

CREATE UNIQUE INDEX uq_usuarios_paciente_id
    ON usuarios (paciente_id)
    WHERE paciente_id IS NOT NULL;

COMMENT ON COLUMN usuarios.paciente_id IS
    'Vínculo persistente da conta de perfil PACIENTE ao registro de paciente; preenchido pelo cadastro transacional.';
