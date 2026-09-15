# autoservice-lambda-auth

Função **Serverless** de autenticação por **CPF** do Tech Challenge POS TECH (SOAT).

Este repositório complementa a aplicação principal: nas Fases 1–2 a API já existia; na **Fase 3** a autenticação do cliente por CPF passa a ser uma **Lambda** invocada pelo **API Gateway**, consultando o **RDS** e emitindo **JWT** para as rotas protegidas.

---

## Propósito

- Validar o CPF informado pelo cliente.
- Consultar existência e status do cliente no PostgreSQL (schema `cadastro`).
- Emitir JWT (**HS256**) com claims `sub` e `cpf` para consumo das APIs protegidas no EKS.
- Expor o contrato HTTP `POST /auth/cpf` via API Gateway (repositório `autoservice-infra-k8s`).

### Ecossistema

| Repositório | Papel |
|-------------|--------|
| **autoservice-lambda-auth** (este) | Lambda `cpf-auth` |
| [autoservice](https://github.com/cristhian-ruescas/autoservice) | API Spring (valida o JWT) |
| [autoservice-infra-k8s](https://github.com/Zainequeiros/autoservice-infra-k8s) | API Gateway + rede/VPC |
| [autoservice-infra-db](https://github.com/Zainequeiros/autoservice-infra-db) | Amazon RDS PostgreSQL |

Diagramas oficiais: [Miro — Autoservice](https://miro.com/app/board/uXjVHprBYf0=/).

---

## Tecnologias

| Item | Escolha |
|------|---------|
| Runtime | Python 3.11+ (CI usa 3.12) |
| Cloud | AWS Lambda |
| Empacotamento / deploy | Serverless Framework (`serverless.yml`, `serverless.vpc.yml`) |
| Token | JWT HS256 (`PyJWT`) |
| Banco | PostgreSQL (RDS) via `psycopg` / variáveis `DB_*` |
| CI/CD | GitHub Actions (`.github/workflows/`) |
| Testes | `pytest` |

**Dockerfile:** não se aplica (artefato = pacote Lambda / zip). A aplicação containerizada fica no repositório `autoservice`.

---

## Diagrama da arquitetura (este repositório)

```text
Cliente
   │  POST /auth/cpf  {"cpf":"..."}
   v
API Gateway (HTTPS)
   │  integração AWS_PROXY / invoke
   v
Lambda cpf-auth  (este repositório)
   │
   ├─ valida formato do CPF
   ├─ SQL no RDS (schema cadastro / pessoa_fisica + cliente)
   │     · 404 se não encontrado
   │     · 403 se inativo
   └─ gera JWT HS256 (sub, cpf, iss, iat, exp)
         │
         v
      200 { token, token_type, expires_in }
```

Sequência completa (auth + andamento): frame **“2. Sequência — Autenticação CPF → JWT”** no [Miro](https://miro.com/app/board/uXjVHprBYf0=/).

Visão cloud com ícones AWS: [frame Arquitetura AWS](https://miro.com/app/board/uXjVHprBYf0=/?moveToWidget=3458764683739809878) · PNG no repo da app: `autoservice/docs/observability/diagrams/`.

---

## Link Swagger / Postman

Esta Lambda **não** publica Swagger próprio. O contrato das APIs protegidas (e a collection Postman) está no repositório da aplicação:

| Recurso | Link |
|---------|------|
| Postman (app) | [Autoservice API.postman_collection.json](https://github.com/cristhian-ruescas/autoservice/blob/develop/Autoservice%20API.postman_collection.json) |
| Swagger (app local) | http://localhost:8088/swagger-ui.html |
| Swagger (homolog) | `https://<api_gateway_url>/swagger-ui.html` (output do `autoservice-infra-k8s`) |

Contrato desta função (abaixo) equivale ao request `POST /auth/cpf` na collection.

---

## Estrutura

```text
src/
  lambda_function.py   # handler
  cpf.py               # validação de CPF
  repository.py        # consulta no schema cadastro
tests/
serverless.yml         # deploy sem VPC (dev/simples)
serverless.vpc.yml     # deploy com VPC até o RDS
.github/workflows/     # CI/CD
requirements.txt
```

---

## Contrato `POST /auth/cpf`

Body:

```json
{ "cpf": "39053344705" }
```

Sucesso (`200`):

```json
{
  "token": "<jwt>",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

| Situação | HTTP |
|----------|------|
| CPF ausente / inválido (formato) | `400` |
| Cliente não encontrado | `404` |
| Cliente inativo | `403` |
| OK | `200` + JWT |

O JWT deve ser validado pela app com o **mesmo** segredo (`JWT_SECRET` aqui = `AUTOSERVICE_JWT_SECRET` no Spring).

---

## Variáveis de ambiente

| Variável | Uso |
|----------|-----|
| `JWT_SECRET` | Segredo HS256 (obrigatório em produção) |
| `JWT_ISSUER` | Padrão `autoservice-auth` |
| `JWT_EXPIRES_SECONDS` | Padrão `3600` |
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` | RDS |
| `LAMBDA_SG_ID`, `LAMBDA_SUBNET_1`, `LAMBDA_SUBNET_2` | VPC (`serverless.vpc.yml`) |
| `AWS_ACCOUNT_ID`, `AWS_REGION` | Conta/região |

---

## Execução local (testes)

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:PYTHONPATH="src"
pytest -q --cov=src --cov-report=term-missing --cov-fail-under=80
```

---

## Deploy

### Manual (Serverless)

```bash
npm install
export AWS_ACCOUNT_ID=...
export JWT_SECRET=...
export DB_HOST=... DB_NAME=... DB_USER=... DB_PASSWORD=...
npx serverless deploy --stage dev

# Com acesso ao RDS em subnets privadas:
# export LAMBDA_SG_ID=... LAMBDA_SUBNET_1=... LAMBDA_SUBNET_2=...
# npx serverless deploy --config serverless.vpc.yml --stage dev
```

Após o deploy, configure no Terraform do `autoservice-infra-k8s` as variáveis `auth_lambda_function_name` e `auth_lambda_invoke_arn` para habilitar a rota no API Gateway.

### CI/CD

Workflow único: **`.github/workflows/ci-cd.yml`** (`name: CI/CD`).

| Evento | Ação |
|--------|------|
| `pull_request` → `develop` / `main` | `pytest` |
| `push` → `develop` | test + deploy **homolog** (`update-function-code` → `cpf-auth`) |
| `push` → `main` | test + deploy **prod** (mesma função no Lab Academy) |

Auth AWS alinhada aos repos de infra: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` (Lab).

Secrets: `JWT_SECRET`, `DB_*`, `AWS_ACCOUNT_ID`, `AWS_REGION`, opcional `LAMBDA_FUNCTION_NAME` (padrão `cpf-auth`).

> Em AWS Academy o Lab encerra credenciais — renovar secrets antes de qualquer deploy automático.

---

## Alinhamento ao desafio (Fase 3)

- [x] Function Serverless: valida CPF, consulta cliente no banco, devolve JWT
- [x] Integração com API Gateway (`POST /auth/cpf`)
- [x] Pipeline CI/CD com testes
- [x] Acesso ao RDS (VPC quando necessário)
- [x] Segredo JWT alinhado à aplicação no EKS

---

## Licença / uso acadêmico

Projeto do **Tech Challenge** (pós-graduação SOAT).
