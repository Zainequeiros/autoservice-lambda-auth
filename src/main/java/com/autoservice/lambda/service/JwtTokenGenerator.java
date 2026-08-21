package com.autoservice.lambda.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

/**
 * Serviço para geração de JWT.
 */
public class JwtTokenGenerator {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenGenerator.class);
    private static final String INSECURE_DEFAULT_SECRET = "change-me-in-production";

    private final String secret;
    private final String issuer;
    private final long expiresInSeconds;
    private final String keyId;

    public JwtTokenGenerator() {
        String secretEnv = readEnv("JWT_SECRET", INSECURE_DEFAULT_SECRET);
        String issuerEnv = readEnv("JWT_ISSUER", "autoservice-auth");
        long expiresEnv = Long.parseLong(readEnv("JWT_EXPIRES_SECONDS", "3600"));
        String keyIdEnv = readEnv("JWT_KEY_ID", "v1");

        validateSecret(secretEnv);
        validateExpiration(expiresEnv);

        this.secret = secretEnv;
        this.issuer = issuerEnv;
        this.expiresInSeconds = expiresEnv;
        this.keyId = keyIdEnv;
    }

    public JwtTokenGenerator(String secret, String issuer, long expiresInSeconds) {
        validateSecret(secret);
        validateExpiration(expiresInSeconds);
        this.secret = secret;
        this.issuer = issuer;
        this.expiresInSeconds = expiresInSeconds;
        this.keyId = "test";
    }

    /**
     * Gera token JWT com claims do cliente.
     *
     * @param cpf CPF do cliente (apenas dígitos)
     * @param customerStatus Status do cliente (ATIVO, INATIVO, etc)
     * @return Token JWT assinado com HS256
     */
    public String generateToken(String cpf, String customerStatus) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresInSeconds);

        String token = Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setSubject(cpf) // sub: CPF do cliente
                .claim("cpf", cpf) // CPF do cliente
                .claim("customer_status", customerStatus) // Status: ATIVO, INATIVO
                .claim("roles", Arrays.asList("CUSTOMER")) // Papéis do cliente
                .setIssuer(issuer) // iss: Issuer
                .setIssuedAt(Date.from(now)) // iat: Issued at
                .setExpiration(Date.from(expiresAt)) // exp: Expiration
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        logger.info("Token gerado com expiração em {}s e key_id={}", expiresInSeconds, keyId);
        return token;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    private static String readEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    private void validateSecret(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET não pode ser vazio.");
        }

        boolean allowInsecure = "true".equalsIgnoreCase(System.getenv("ALLOW_INSECURE_JWT_SECRET"));
        if (INSECURE_DEFAULT_SECRET.equals(rawSecret) && !allowInsecure) {
            throw new IllegalStateException("JWT_SECRET inseguro. Defina segredo forte ou use ALLOW_INSECURE_JWT_SECRET=true apenas em ambiente local.");
        }

        if (rawSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter no mínimo 32 bytes.");
        }
    }

    private void validateExpiration(long expirationSeconds) {
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("JWT_EXPIRES_SECONDS deve ser maior que zero.");
        }
    }
}
