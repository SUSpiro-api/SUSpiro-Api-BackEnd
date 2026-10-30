package com.app.suspiro.dto;

import java.time.LocalDateTime;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;

public record UsuarioResponse(
        Long id,
        String cpf,
        String email,
        String nome,
        Perfil perfil,
        String sexo,
        String registroProfissional,
        boolean ativo,
        boolean senhaProvisoria,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(UsuariosModel usuario) {

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getNome(),
                usuario.getPerfil(),
                usuario.getSexo(),
                usuario.getRegistroProfissional(),
                usuario.isAtivo(),
                usuario.isSenhaProvisoria(),
                usuario.getCriadoEm()
        );
    }
}
