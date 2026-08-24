package com.app.suspiro.config;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;

import io.github.cdimascio.dotenv.Dotenv;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminRoot implements CommandLineRunner {

    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminRoot(
            UsuariosRepository usuariosRepository,
            PasswordEncoder passwordEncoder) {

        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        System.out.println("=================================");
        System.out.println("ADMIN ROOT INICIADO");
        System.out.println("=================================");

        boolean existe =
                usuariosRepository.existsByPerfil(Perfil.SUPER_ADMIN);

        System.out.println("SUPER_ADMIN EXISTE? " + existe);

        if (existe) {
            System.out.println("Super Admin já existe.");
            return;
        }

        System.out.println("Super Admin ainda não existe.");
        System.out.println("Lendo arquivo .env...");

        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        String cpf = dotenv.get("SUPER_ADMIN_CPF");
        String nome = dotenv.get("SUPER_ADMIN_NOME");
        String senha = dotenv.get("SUPER_ADMIN_PASSWORD");

        System.out.println("CPF encontrado? " + (cpf != null));
        System.out.println("NOME encontrado? " + (nome != null));
        System.out.println("SENHA encontrada? " + (senha != null));

        if (cpf == null || nome == null || senha == null) {

            throw new IllegalStateException(
                    "Variáveis SUPER_ADMIN_* não encontradas no arquivo .env"
            );
        }

        UsuariosModel superAdmin = new UsuariosModel();

        superAdmin.setCpf(cpf);
        superAdmin.setNome(nome);
        superAdmin.setSenha(passwordEncoder.encode(senha));
        superAdmin.setPerfil(Perfil.SUPER_ADMIN);
        superAdmin.setAtivo(true);

        usuariosRepository.save(superAdmin);

        System.out.println("=================================");
        System.out.println("SUPER ADMIN CRIADO COM SUCESSO!");
        System.out.println("NOME: " + nome);
        System.out.println("=================================");
    }
}