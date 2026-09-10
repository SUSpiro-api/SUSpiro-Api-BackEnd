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
        boolean ativo,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(UsuariosModel usuario) {

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getNome(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.getCriadoEm()
        );
    }
}
