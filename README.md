# SUSpiro API — Backend integrado

Backend Spring Boot do SUSpiro, com autenticação JWT, usuários, pacientes, guias, requisições, agendamentos, acompanhamento por protocolo e envio de e-mails.

## Requisitos

- Java 21
- PostgreSQL compatível com o driver do projeto
- Uma conta SMTP válida para envio de senhas provisórias e notificações
- Maven (ou o Maven Wrapper do projeto, caso o ambiente consiga baixar a distribuição)

## Configuração local

1. Crie um banco PostgreSQL dedicado a esta versão. O nome padrão do arquivo de exemplo é `suspiro_db`. Se for usar outro banco, como `suspiro_central`, altere `DB_NAME` para corresponder ao nome real.
2. Copie `.env.exemplo` para `.env`.
3. Preencha as variáveis do PostgreSQL, `JWT_SECRET` (chave aleatória forte com pelo menos 32 bytes), `JWT_EXPIRATION` (por exemplo, `86400000` para 24 horas), `SUPER_ADMIN_CPF` (11 dígitos, sem pontuação), `SUPER_ADMIN_PASSWORD`, `SUPER_ADMIN_NOME` e as credenciais SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`). Não use os valores de exemplo em produção.
4. Para Gmail, use uma senha de app SMTP, não a senha normal da conta. Configure `FRONTEND_URL=http://localhost:5173` para desenvolvimento local.
5. Inicie o backend na raiz do projeto. No PowerShell, execute `.\mvnw.cmd spring-boot:run`. Se o wrapper não conseguir baixar Maven, instale Maven e execute `mvn spring-boot:run`.

O Flyway executa as migrações `V1` até `V9`. A migração `V9__patient_account_and_insurance.sql` habilita o perfil `PACIENTE` e adiciona o campo `convenio`; ela altera o esquema, então faça backup antes de reaproveitar um banco com dados existentes.

## Frontend

Na pasta do frontend, copie `.env.example` para `.env` e mantenha:

```env
VITE_API_URL=/api
VITE_AUTH_MODE=legacy-bearer-memory
VITE_GUIDE_DATA_SOURCE=api
```

O servidor Vite encaminha `/api/*` para `http://localhost:8080/*`. Inicie a API e execute `npm ci` e `npm run dev` na pasta do frontend.

## Cadastro de paciente e primeiro acesso

`POST /pacientes` exige um usuário profissional autenticado. O cadastro exige nome, CPF, data de nascimento e e-mail; convênio e telefone são opcionais. O backend cria o registro do paciente e uma conta com perfil `PACIENTE`. A senha inicial é a data de nascimento no formato `DDMMAAAA`, armazenada somente como hash BCrypt. A conta é marcada como senha provisória; no primeiro login, um modal obrigatório solicita uma senha forte antes de liberar outras rotas protegidas. A data de nascimento é previsível, então deve ser usada apenas como credencial temporária e trocada imediatamente.

## Endpoints principais

- `POST /auth/login` — login por CPF e senha; devolve JWT, perfil do usuário e indicador de senha provisória.
- `POST /auth/password/forgot` — solicita link de recuperação, enviado para o e-mail associado à conta quando disponível; a resposta não revela se o CPF existe.
- `POST /auth/password/reset` — troca a senha usando token único, armazenado como hash e válido por 30 minutos.
- `POST /usuarios` — criação de funcionários com senha provisória enviada por e-mail.
- `GET/PATCH /usuarios/me` — consulta e atualização do nome da conta.
- `PATCH /usuarios/me/senha` — troca obrigatória ou voluntária de senha.
- `POST /pacientes`, `GET /pacientes?cpf=...`, `GET /pacientes?busca=...` — cadastro e busca de pacientes por usuários autorizados.
- `POST /guias`, `GET /guias`, `GET /guias/minhas`, `GET /guias/pendentes`, `PATCH /guias/{id}/agendamento` — fluxo de guias.
- `GET /requisicoes`, `POST /requisicoes`, `GET /requisicoes/minhas`, `POST /requisicoes/{id}/agendamento`, `PATCH /requisicoes/{id}/conclusao` — fluxo de requisições.
- `GET /acompanhamento?protocolo=...&cpf=...` — acompanhamento público mediante protocolo e CPF.

A documentação OpenAPI está disponível em `http://localhost:8080/swagger-ui.html` quando o backend está em execução.

## Recuperação por e-mail

O cadastro do paciente não envia e-mail e não depende do SMTP para ser salvo. O endereço fornecido é usado na recuperação de senha: a pessoa informa o CPF e recebe um link no e-mail associado à conta. O link é válido por 30 minutos e pode ser usado uma única vez. Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` e `MAIL_FROM` no arquivo `.env`; para Gmail, use uma Senha de app com verificação em duas etapas, não a senha comum. Falhas de entrega aparecem nos logs do backend, enquanto a resposta pública de recuperação permanece genérica para não revelar se um CPF está cadastrado.
