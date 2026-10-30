package com.app.suspiro.enums;

public enum Perfil {
    SUPER_ADMIN,   // 0 Perfil dono do sistema (único que gerencia Gerentes e todos o demais)
    GERENTE,       // 1 Perfil com nível de gestão total do sistema
    MEDICO,        // 2 Perfil assistencial técnico de nível superior (visualiza/edita suas requisicoes/cria)
    ENFERMEIRO,    // 3 Perfil assistencial técnico e de triagem (visualiza/edita suas requisicoes/cria)
    ATENDENTE,     // 4 Perfil responsável pela recepção, quem de fato irá consumar a requisição
    PACIENTE       // 5 Conta de acesso do próprio paciente
}