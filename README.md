# autoservice-lambda-auth

Funcao Serverless de autenticacao por CPF para o Tech Challenge POS TECH.

## Proposito

Este repositorio contem a Lambda responsavel por:
- validar o CPF do cliente;
- consultar a existencia e o status do cliente no RDS;
- emitir JWT (HS256) com claims `sub` e `cpf` para rotas protegidas.

## Alinhamento ao desafio corporativo

- API Gateway (`autoservice-infra-k8s`) expoe `POST /auth/cpf` e invoca esta Lambda.
- A aplicacao no EKS consome o JWT (role `CLIENTE` no andamento da OS).
- Deploy com VPC ate o RDS: `serverless.vpc.yml`.
- Pipeline CI/CD valida testes e faz deploy por ambiente.
- Branch protection com PR obrigatoria nas branches principais.

## Tecnologias

- Python 3.11
- AWS Lambda
- Serverless Framework
- JWT (HS256)
- PostgreSQL (RDS) via variaveis `DB_*`

## Estrutura

- `src/lambda_function.py`: handler
- `src/cpf.py`: validacao de CPF
- `src/repository.py`: consulta no schema `cadastro`
- `serverless.yml`: deploy da function
- `serverless.vpc.yml`: deploy com VPC (subnets/SG)
- `.github/workflows/`: CI/CD

## Execucao local

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt pytest
pytest -q
```

## Contrato

`POST /auth/cpf` com body `{"cpf":"..."}`.

Sucesso:

```json
{
  "statusCode": 200,
  "body": "{\"token\":\"<jwt>\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
}
```

## Variaveis de ambiente

| Variavel | Uso |
|----------|-----|
| `JWT_SECRET` | Segredo HS256 (mesmo da app) |
| `JWT_ISSUER` | Padrao `autoservice-auth` |
| `JWT_EXPIRES_SECONDS` | Padrao `3600` |
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` | RDS |
| `LAMBDA_SG_ID`, `LAMBDA_SUBNET_1`, `LAMBDA_SUBNET_2` | VPC (`serverless.vpc.yml`) |
| `AWS_ACCOUNT_ID`, `AWS_REGION` | Conta/regiao do deploy |

## Deploy

```bash
npm install
export AWS_ACCOUNT_ID=...
export JWT_SECRET=...
export DB_HOST=... DB_NAME=... DB_USER=... DB_PASSWORD=...
npx serverless deploy --stage dev
# Com VPC:
# export LAMBDA_SG_ID=... LAMBDA_SUBNET_1=... LAMBDA_SUBNET_2=...
# npx serverless deploy --config serverless.vpc.yml --stage dev
```

## CI/CD

Secrets esperados: `AWS_ROLE_TO_ASSUME`, `AWS_REGION`, `JWT_SECRET` (e opcionais de JWT).
