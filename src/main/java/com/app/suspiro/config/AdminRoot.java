package com.app.suspiro.config;

import com.app.suspiro.enums.Perfil;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.repository.UsuariosRepository;
import com.app.suspiro.util.CpfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminRoot implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminRoot.class);

    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;
    private final String cpfConfigurado;
    private final String nome;
    private final String senha;

    public AdminRoot(
            UsuariosRepository usuariosRepository,
            PasswordEncoder passwordEncoder,
            @Value("${SUPER_ADMIN_CPF:}") String cpfConfigurado,
            @Value("${SUPER_ADMIN_NOME:}") String nome,
            @Value("${SUPER_ADMIN_PASSWORD:}") String senha) {
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
        this.cpfConfigurado = CpfUtil.normalizar(cpfConfigurado);
        this.nome = nome;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (CpfUtil.valido(cpfConfigurado)) {
            var contaConfigurada = usuariosRepository.findByCpf(cpfConfigurado);
            if (contaConfigurada.isPresent()) {
                UsuariosModel conta = contaConfigurada.get();
                if (conta.getPerfil() != Perfil.SUPER_ADMIN) {
                    throw new IllegalStateException(
                            "SUPER_ADMIN_CPF já pertence a uma conta que não é SUPER_ADMIN. "
                            + "Configure o CPF de uma conta administrativa dedicada.");
                }
                return;
            }
        }

        if (usuariosRepository.existsByPerfil(Perfil.SUPER_ADMIN)) {
            log.info("Uma conta SUPER_ADMIN já existe. Nenhuma conta adicional foi criada.");
            return;
        }

        if (!CpfUtil.valido(cpfConfigurado) || nome == null || nome.isBlank()
                || senha == null || senha.length() < 12) {
            throw new IllegalStateException("Configure SUPER_ADMIN_CPF válido, SUPER_ADMIN_NOME e SUPER_ADMIN_PASSWORD com pelo menos 12 caracteres.");
        }

        UsuariosModel superAdmin = new UsuariosModel();
        superAdmin.setCpf(cpfConfigurado);
        superAdmin.setNome(nome.trim());
        superAdmin.setSenha(passwordEncoder.encode(senha));
        superAdmin.setPerfil(Perfil.SUPER_ADMIN);
        superAdmin.setAtivo(true);
        superAdmin.setSenhaProvisoria(false);
        usuariosRepository.save(superAdmin);
        log.info("Conta inicial SUPER_ADMIN criada.");
    }
}
