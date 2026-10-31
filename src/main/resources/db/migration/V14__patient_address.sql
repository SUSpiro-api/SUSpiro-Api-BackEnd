-- Endereço estruturado do paciente. Campos antigos permanecem NULL até atualização cadastral.
ALTER TABLE pacientes
    ADD COLUMN cep VARCHAR(8),
    ADD COLUMN logradouro VARCHAR(180),
    ADD COLUMN numero VARCHAR(20),
    ADD COLUMN complemento VARCHAR(120),
    ADD COLUMN bairro VARCHAR(120),
    ADD COLUMN cidade VARCHAR(120),
    ADD COLUMN estado VARCHAR(2),
    ADD COLUMN atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_pacientes_cidade_estado ON pacientes (UPPER(cidade), UPPER(estado));
COMMENT ON COLUMN pacientes.cep IS 'CEP normalizado, somente 8 dígitos.';
COMMENT ON COLUMN pacientes.estado IS 'Sigla da unidade federativa, duas letras maiúsculas.';
