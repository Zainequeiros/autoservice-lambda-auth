# RFC-001 - Observabilidade da Lambda e gestão de segredos

- **Status:** Proposta
- **Data:** 2026-08-21
- **Autores:** Time Tech Challenge

## 1. Problema

O Tech Challenge exige visibilidade operacional (latência, erros, uptime, traces e correlação) e hardening de segurança para autenticação.

## 2. Proposta

## 2.1 Observabilidade

1. Padronizar logs estruturados em JSON com correlação por `request_id` e `correlation_id`.
2. Registrar `duration_ms`, `cold_start`, `statusCode` e `cpf_hash` (sem CPF em claro).
3. Integrar pipeline de ingestão de logs para Datadog ou New Relic no ambiente.

## 2.2 Métricas mínimas obrigatórias

- Latência p95/p99 da autenticação.
- Taxa de erro (4xx/5xx).
- Tempo de execução médio.
- Percentual de cold starts.
- Disponibilidade do endpoint de autenticação.

## 2.3 Gestão de segredos

1. `JWT_SECRET` obrigatório com mínimo de 32 bytes.
2. Bloqueio de segredo inseguro default em runtime.
3. Uso de `JWT_KEY_ID` para rastrear chave ativa.
4. Segredos por ambiente (`homolog` e `prod`) em GitHub Environments e/ou AWS Secrets Manager/SSM.

## 3. Impacto esperado

- Melhor tempo de diagnóstico e correlação ponta a ponta.
- Redução de risco operacional de autenticação.
- Base para auditoria de segurança e operação.

## 4. Dependências externas

- Configuração de forwarder/integração Datadog ou New Relic.
- Dashboards e alertas centralizados do programa (fora deste repositório).
