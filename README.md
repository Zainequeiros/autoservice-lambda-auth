# autoservice-lambda-auth

Função serverless de autenticação por **CPF**: valida o documento, consulta o cliente no PostgreSQL e emite **JWT** para as APIs protegidas.

## Propósito

- Validar CPF
- Consultar existência/status no schema `cadastro`
- Emitir JWT HS256 (`sub`, `cpf`)
- Contrato HTTP `POST /auth/cpf` (via API Gateway no `autoservice-infra-k8s`)

## Ecossistema

| Repositório | Papel |
|-------------|--------|
| **autoservice-lambda-auth** (este) | Lambda `cpf-auth` |
| Apps Spring (OS / billing / execução / monólito) | Validam o JWT |
| `autoservice-infra-k8s` | API Gateway + VPC |
| `autoservice-infra-db` | RDS PostgreSQL |

## Tecnologias

| Item | Escolha |
|------|---------|
| Runtime | Python 3.11+ |
| Cloud | AWS Lambda |
| Empacotamento | Serverless Framework |
| Token | JWT HS256 (`PyJWT`) |
| Banco | PostgreSQL via `DB_*` |
| CI/CD | GitHub Actions (`.github/workflows/ci-cd.yml`) |
| Testes | `pytest` (cobertura ≥ 80%) |

## Contrato `POST /auth/cpf`

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
| CPF ausente / inválido | `400` |
| Cliente não encontrado | `404` |
| Cliente inativo | `403` |
| OK | `200` |

O segredo `JWT_SECRET` deve ser o mesmo usado pelas APIs (`AUTOSERVICE_JWT_SECRET`).

## Testes locais

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:PYTHONPATH="src"
pytest -q --cov=src --cov-report=term-missing --cov-fail-under=80
```

## Deploy

```bash
npm install
# export AWS_ACCOUNT_ID, JWT_SECRET, DB_*
npx serverless deploy --stage dev
# Com VPC até o RDS: serverless.vpc.yml + LAMBDA_SG_ID / LAMBDA_SUBNET_*
```

### CI/CD

| Evento | Ação |
|--------|------|
| Pull request | Test |
| Push | Test + Deploy (homolog/prod conforme branch) |
