-- Allow the new patient account role and the insurance field used by the current UI.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS chk_usuarios_perfil;
ALTER TABLE usuarios
    ADD CONSTRAINT chk_usuarios_perfil
    CHECK (perfil IN ('SUPER_ADMIN', 'GERENTE', 'ENFERMEIRO', 'MEDICO', 'ATENDENTE', 'PACIENTE'));

ALTER TABLE pacientes ALTER COLUMN numero_sus DROP NOT NULL;
ALTER TABLE pacientes DROP CONSTRAINT IF EXISTS pacientes_numero_sus_key;
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS convenio VARCHAR(120);

COMMENT ON COLUMN pacientes.convenio IS
    'Convênio informado no cadastro do paciente; opcional.';
