package com.autoservice.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.autoservice.lambda.dto.AuthResponse;
import com.autoservice.lambda.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler principal da função Lambda de autenticação.
 */
public class AuthHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private static final AtomicBoolean COLD_START = new AtomicBoolean(true);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthService authService = new AuthService();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        long start = System.currentTimeMillis();
        boolean coldStart = COLD_START.getAndSet(false);

        String requestId = context != null ? context.getAwsRequestId() : UUID.randomUUID().toString();
        String correlationId = resolveCorrelationId(event);

        MDC.put("request_id", requestId);
        MDC.put("correlation_id", correlationId);
        MDC.put("cold_start", String.valueOf(coldStart));

        logger.info("Lambda invocada para autenticação");

        try {
            // Parsear body
            String bodyRaw = (String) event.get("body");
            JsonNode body = null;
            
            if (bodyRaw != null) {
                body = objectMapper.readTree(bodyRaw);
            }

            String cpfInput = body != null && body.has("cpf") ? body.get("cpf").asText() : null;

            // Autenticar
            AuthResponse authResponse = authService.authenticate(cpfInput);

            // Retornar sucesso
            return buildResponse(200, authResponse, correlationId);

        } catch (AuthService.AuthException e) {
            logger.warn("Erro de autenticação: {}", e.getMessage());
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return buildResponse(e.getStatusCode(), errorBody, correlationId);

        } catch (Exception e) {
            logger.error("Erro inesperado na Lambda", e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Erro interno do servidor");
            return buildResponse(500, errorBody, correlationId);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            MDC.put("duration_ms", String.valueOf(durationMs));
            logger.info("Execução da Lambda finalizada");
            MDC.clear();
        }
    }

    /**
     * Constrói uma resposta HTTP no formato esperado pelo API Gateway.
     */
    private Map<String, Object> buildResponse(int statusCode, Object body, String correlationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Correlation-Id", correlationId);
        response.put("headers", headers);

        try {
            response.put("body", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            logger.error("Erro ao serializar response", e);
            response.put("body", "{\"message\": \"Erro ao serializar resposta\"}");
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private String resolveCorrelationId(Map<String, Object> event) {
        if (event == null) {
            return UUID.randomUUID().toString();
        }

        Object headersObj = event.get("headers");
        if (!(headersObj instanceof Map<?, ?> headersRaw)) {
            return UUID.randomUUID().toString();
        }

        Map<String, Object> headers = (Map<String, Object>) headersRaw;
        Object correlation = headers.getOrDefault("x-correlation-id",
                headers.getOrDefault("X-Correlation-Id", null));

        if (correlation == null && !headers.isEmpty()) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                if ("x-correlation-id".equalsIgnoreCase(entry.getKey())) {
                    correlation = entry.getValue();
                    break;
                }
            }
        }

        if (correlation == null) {
            return UUID.randomUUID().toString();
        }

        String correlationId = String.valueOf(correlation).trim();
        return correlationId.isEmpty() ? UUID.randomUUID().toString() : correlationId;
    }
}
