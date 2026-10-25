package com.app.suspiro.config;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.util.CpfUtil;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminRoot implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminRoot.class);
    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminRoot(UsuariosRepository usuariosRepository, PasswordEncoder passwordEncoder) {
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuariosRepository.existsByPerfil(Perfil.SUPER_ADMIN)) {
            log.info("Conta inicial SUPER_ADMIN já existe; nenhuma alteração foi feita.");
            return;
        }

        Dotenv dotenv = Dotenv.configure().directory("./").filename(".env").ignoreIfMissing().load();
        String cpf = CpfUtil.normalizar(dotenv.get("SUPER_ADMIN_CPF"));
        String nome = dotenv.get("SUPER_ADMIN_NOME");
        String senha = dotenv.get("SUPER_ADMIN_PASSWORD");

        if (!CpfUtil.valido(cpf) || nome == null || nome.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalStateException("Configure SUPER_ADMIN_CPF válido, SUPER_ADMIN_NOME e SUPER_ADMIN_PASSWORD no .env antes da primeira inicialização.");
        }

        UsuariosModel superAdmin = new UsuariosModel();
        superAdmin.setCpf(cpf);
        superAdmin.setNome(nome.trim());
        superAdmin.setSenha(passwordEncoder.encode(senha));
        superAdmin.setPerfil(Perfil.SUPER_ADMIN);
        superAdmin.setAtivo(true);
        superAdmin.setSenhaProvisoria(false);
        usuariosRepository.save(superAdmin);
        log.info("Conta inicial SUPER_ADMIN criada.");
    }
}
