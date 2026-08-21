# Status de Aderência ao Tech Challenge - Repositório `autoservice-lambda-auth`

Data de referência: 2026-08-21

## 1) Objetivo deste documento

Consolidar, para o time, o que já foi implementado neste repositório de **Lambda de autenticação**, por que cada item foi feito e o que ainda precisa ser concluído para aderência total ao escopo do Tech Challenge.

> Escopo analisado: **apenas este repositório** (`autoservice-lambda-auth`).  
> Itens que dependem dos outros 3 repositórios aparecem como integração/pendência externa.

---

## 2) Resumo executivo

Este repositório já atende o núcleo da exigência de **Function Serverless para autenticação via CPF**:
- valida CPF;
- consulta cliente e status no banco;
- gera JWT para consumo das APIs protegidas;
- possui pipeline CI/CD com build, testes e deploy automático para `homolog` e `prod`.

As principais lacunas neste repositório agora estão concentradas em:
- **integração operacional final com Datadog/New Relic** (forwarder/agent, dashboards e alertas no ambiente);
- **evidência implementada de API Gateway** (aqui há contrato/preparo e documentação, mas não IaC/definição de gateway neste repo);
- **decisão final de gestão centralizada de segredos** do grupo (Secrets Manager/Parameter Store por ambiente).

---

## 3) O que já foi feito e por quê

## 3.1 Function Serverless de autenticação por CPF (Requisito obrigatório)

### O que foi implementado
- Handler Lambda: `src/main/java/com/autoservice/lambda/AuthHandler.java`
- Serviço de autenticação: `src/main/java/com/autoservice/lambda/service/AuthService.java`
- Validação de CPF: `src/main/java/com/autoservice/lambda/util/CpfValidator.java`
- Consulta ao banco: `src/main/java/com/autoservice/lambda/repository/CustomerRepository.java`
- Conexão com PostgreSQL (RDS): `src/main/java/com/autoservice/lambda/repository/DatabaseConnection.java`
- Geração de token JWT: `src/main/java/com/autoservice/lambda/service/JwtTokenGenerator.java`
- DTOs de contrato: `src/main/java/com/autoservice/lambda/dto/AuthRequest.java` e `AuthResponse.java`

### Por que foi feito
- Implementa o fluxo de segurança exigido para autenticação com CPF.
- Separa responsabilidades (handler, serviço, repositório, utilitários), facilitando manutenção e testes.
- Permite integrar a aplicação principal com rotas protegidas por token JWT.

### Resultado prático
- Retorna respostas padronizadas de sucesso e erro (`200`, `400`, `403`, `404`, `500`) no formato esperado por API Gateway.

---

## 3.2 Segurança e emissão de JWT

### O que foi implementado
- Geração de JWT assinado (HS256) com claims de cliente (`sub`, `cpf`, `customer_status`, `roles`, `iss`, `iat`, `exp`) e header `kid`.
- Configuração por variáveis de ambiente (`JWT_SECRET`, `JWT_ISSUER`, `JWT_EXPIRES_SECONDS`, `JWT_KEY_ID`).
- Validação de segurança do `JWT_SECRET` (mínimo de 32 bytes) e bloqueio do default inseguro em runtime.
- Validação no CI/CD para impedir deploy com secret fraco.

### Por que foi feito
- Atende à exigência de devolver token válido para consumo das APIs protegidas.
- Permite parametrização por ambiente sem hardcode de configuração operacional.

### Atenção
- O uso de fallback inseguro só deve ocorrer em ambiente local controlado.

---

## 3.3 Integração com banco de dados gerenciado (lado consumo)

### O que foi implementado
- Consumo de PostgreSQL via JDBC + HikariCP.
- Lookup de cliente por CPF com status de ativação.
- Variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

### Por que foi feito
- A Lambda precisa validar existência e status do cliente na base.
- Pool de conexão reduz custo de abertura de conexões e melhora desempenho em invocações recorrentes.

---

## 3.4 Qualidade e testes automatizados

### O que foi implementado
- Testes unitários com JUnit5/Mockito:
  - `src/test/java/com/autoservice/lambda/util/CpfValidatorTest.java`
  - `src/test/java/com/autoservice/lambda/util/SensitiveDataHasherTest.java`
  - `src/test/java/com/autoservice/lambda/service/AuthServiceTest.java`
  - `src/test/java/com/autoservice/lambda/service/JwtTokenGeneratorTest.java`
- Cobertura com JaCoCo.

### Por que foi feito
- Garante robustez no fluxo crítico de autenticação.
- Dá segurança para evolução e refatorações sem quebrar comportamento.

---

## 3.5 CI/CD com deploy automático (homolog e prod)

### O que foi implementado
- Workflow GitHub Actions: `.github/workflows/ci-cd.yml`
- Em PR para `homolog`/`prod`: build + testes + artefatos.
- Em push para `homolog`/`prod`: empacota JAR, faz deploy na Lambda, atualiza configuração e executa smoke test.

### Por que foi feito
- Atende à exigência de pipeline funcional com deploy automático por ambiente.
- Padroniza promoção de mudanças com validação automatizada.

---

## 3.6 Documentação no repositório

### O que foi implementado
- `README.md` com:
  - propósito;
  - tecnologias;
  - execução/build/deploy;
  - diagrama Mermaid do repositório;
  - contrato da Lambda;
  - variáveis e wiring com demais repositórios.
- `STRUCTURE.md` com estrutura de pastas/código.
- Documentação arquitetural complementar:
  - `docs/adr/ADR-001-jwt-auth-strategy.md`
  - `docs/rfc/RFC-001-observability-and-secrets.md`
  - `docs/architecture/auth-sequence.md`

### Por que foi feito
- Atende às exigências de clareza de operação e handoff entre equipes/repositórios.

---

## 4) Aderência aos requisitos do Tech Challenge (neste repositório)

| Requisito | Status neste repo | Observação |
|---|---|---|
| Function Serverless para autenticação | **Atendido** | Implementado e testado |
| Validar CPF | **Atendido** | `CpfValidator` + fluxo no `AuthService` |
| Consultar existência/status no banco | **Atendido** | `CustomerRepository` + `DatabaseConnection` |
| Gerar/devolver JWT | **Atendido** | `JwtTokenGenerator` + `AuthResponse` |
| CI/CD com deploy automático homolog/prod | **Atendido** | Workflow pronto em GitHub Actions |
| API Gateway implementado | **Parcial (indireto)** | Contrato e integração esperada; gateway em si depende de outro repo/IaC |
| Observabilidade (Datadog/New Relic) | **Parcial** | Logs e padrão de monitoramento prontos; falta integração operacional no ambiente |
| Logs estruturados JSON + correlação | **Atendido neste repo** | Logback JSON + `request_id`/`correlation_id`/`cpf_hash`/`duration_ms`/`cold_start` |
| Dockerfile (quando aplicável) | **Não aplicável** | Entrega é JAR para AWS Lambda |
| RFC/ADR/Sequência/ER (arquitetura completa) | **Parcial** | ADR/RFC/sequência criados aqui; ER e visão completa seguem distribuídos no programa |

---

## 5) O que ainda precisa ser implementado (ações objetivas)

## 5.1 Itens internos concluídos/parciais neste repositório (`autoservice-lambda-auth`)

1. **Observabilidade da Lambda** (**parcial**)
   - Padrão mínimo de monitoramento definido no README (latência, taxa de erro, tempo de execução, cold starts).
   - Logs preparados para ingestão por Datadog/New Relic com campos estruturados.
   - **Pendente:** ativação final do forwarder/integração da plataforma no ambiente cloud.

2. **Logs estruturados** (**concluído no código**)
   - `logback.xml` alterado para JSON.
   - Correlação implementada com `request_id`, `correlation_id`, `duration_ms`, `cold_start` e `cpf_hash` (sem CPF em claro).
   - Header `X-Correlation-Id` retornado na resposta da Lambda.

3. **Documentação técnica complementar** (**concluído neste repo**)
   - ADR criado: `docs/adr/ADR-001-jwt-auth-strategy.md`.
   - RFC criada: `docs/rfc/RFC-001-observability-and-secrets.md`.
   - Diagrama de sequência criado: `docs/architecture/auth-sequence.md`.

4. **Hardening de segurança operacional** (**parcial**)
   - `JWT_SECRET` agora é validado com mínimo de 32 bytes e bloqueio de secret default inseguro.
   - `JWT_KEY_ID` incluído para suporte a rotação/versionamento de chave.
   - Pipeline valida secret forte antes de deploy.
   - **Pendente:** consolidar política final do grupo para Secrets Manager/Parameter Store por ambiente.

## 5.2 Dependências de integração com os outros repositórios

1. **`autoservice-infra-k8s`**
   - Evidenciar rota do API Gateway/Ingress para endpoint de auth.
   - Garantir proteção das rotas sensíveis consumindo JWT emitido por esta Lambda.

2. **`autoservice-infra-db`**
   - Garantir tabela/índices/constraints compatíveis com consulta por CPF e status.
   - Formalizar justificativa de modelagem e performance (incluindo ER).

3. **`autoservice` (app principal)**
   - Validar token emitido pela Lambda (`iss`, assinatura, expiração, papéis).
   - Proteger endpoints sensíveis com middleware/autorizador.

---

## 6) Itens de governança que precisam comprovação no GitHub (fora do código)

Estes pontos são obrigatórios no trabalho, mas não ficam totalmente provados por arquivo de código:
- branch `main/master` protegida;
- merge apenas via Pull Request;
- usuário `soat-architecture` adicionado aos 4 repositórios.

**Ação:** capturar evidências (print/config) para o PDF final.

---

## 7) Checklist sugerido para fechamento da entrega

- [ ] Confirmar API Gateway ativo apontando para esta Lambda (homolog/prod).
- [ ] Implementar observabilidade (Datadog/New Relic) com métricas + traces.
- [x] Publicar logs JSON com correlação de requisições.
- [x] Consolidar RFCs/ADRs/diagramas (sequência e decisões da Lambda).
- [ ] Validar consumo de JWT na aplicação principal.
- [ ] Validar pipelines dos 4 repositórios com deploy automatizado.
- [ ] Preparar vídeo (<=15 min) demonstrando autenticação, pipeline, deploy, monitoramento e traces.
- [ ] Consolidar PDF único com todos os links e comprovações.

---

## 8) Conclusão

O repositório `autoservice-lambda-auth` já entrega a parte central de autenticação serverless por CPF com emissão de JWT, hardening de segredo, logs JSON correlacionados e CI/CD automatizado por ambiente.  
Para aderência completa ao escopo global do Tech Challenge, o grupo ainda precisa fechar principalmente a **integração operacional de observabilidade (Datadog/New Relic)**, a **evidência operacional do gateway/proteção de rotas** e a **integração arquitetural final entre os 4 repositórios**.
