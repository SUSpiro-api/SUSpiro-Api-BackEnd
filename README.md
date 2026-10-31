# SUSpiro — Backend

API REST do SUSpiro, construída com Java 21 e Spring Boot. É responsável por autenticação, autorização por perfil, regras de negócio, persistência PostgreSQL, migrações Flyway e notificações por e-mail.

## Desenvolvimento

Configure o arquivo `.env` com base em `.env.exemplo` e inicie a API:

```bash
./mvnw spring-boot:run
```

No Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

## Testes

```bash
./mvnw test
./mvnw verify
```

Swagger/OpenAPI permanece desativado por padrão. Quando ativado com `SWAGGER_ENABLED=true`, o acesso é restrito ao perfil `SUPER_ADMIN`.

Para a visão geral do sistema, os requisitos, variáveis de ambiente, comandos do frontend, segurança e checklist de implantação, consulte o [README principal](../README.md).
