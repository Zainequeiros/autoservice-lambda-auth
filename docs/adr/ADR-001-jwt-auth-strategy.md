# ADR-001 - Estratégia de autenticação com JWT na Lambda de CPF

- **Status:** Aceito
- **Data:** 2026-08-21
- **Contexto:** A aplicação precisa proteger rotas sensíveis com autenticação por CPF, mantendo desacoplamento entre API protegida e mecanismo de login.

## Decisão

Adotar JWT assinado com HS256 emitido pela Lambda de autenticação, com claims:
- `sub` e `cpf` (identidade do cliente);
- `customer_status`;
- `roles` (ex.: `CUSTOMER`);
- `iss`, `iat`, `exp`;
- header `kid` para versionamento da chave.

## Justificativa

1. Modelo stateless para escalar autenticação sem sessão em servidor.
2. Compatível com API Gateway, Kubernetes e múltiplos consumidores.
3. `kid` + `JWT_KEY_ID` permite trilha de rotação de segredo.
4. Expiração curta (`JWT_EXPIRES_SECONDS`) reduz janela de risco.

## Consequências

- O consumidor da API deve validar assinatura, `iss`, expiração e regras de autorização.
- `JWT_SECRET` deve ser gerenciado por segredo seguro e possuir no mínimo 32 bytes.
- Rotação exige coordenação entre emissor (Lambda) e validadores (aplicação principal/API).
