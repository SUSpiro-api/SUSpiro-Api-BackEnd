ALTER TABLE pacientes
    ADD COLUMN email VARCHAR(255);

COMMENT ON COLUMN pacientes.email IS
    'E-mail para onde sao enviados os avisos de acompanhamento das requisicoes do paciente. Opcional: sem ele, o paciente acompanha apenas pelo protocolo.';
