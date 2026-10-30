

ALTER TABLE pacientes
    ALTER COLUMN id DROP DEFAULT;

DROP SEQUENCE IF EXISTS pacientes_id_seq;

ALTER TABLE pacientes
    ALTER COLUMN id TYPE UUID USING gen_random_uuid();

ALTER TABLE pacientes
    ALTER COLUMN id SET DEFAULT gen_random_uuid();
