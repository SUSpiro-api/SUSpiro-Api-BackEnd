-- Remove o cadastro dinâmico de hospitais/clínicas.
-- O histórico de cada guia permanece nos campos estabelecimento_nome/endereco/cidade/estado.
DROP INDEX IF EXISTS idx_guias_estabelecimento;
ALTER TABLE guias DROP COLUMN IF EXISTS estabelecimento_id;
DROP TABLE IF EXISTS estabelecimentos_saude;
