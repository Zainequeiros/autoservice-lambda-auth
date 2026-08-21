package com.autoservice.lambda.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes para geração de JWT.
 */
class JwtTokenGeneratorTest {

    private JwtTokenGenerator tokenGenerator;
    private static final String SECRET = "test-secret-key-for-testing-purposes";
    private static final String ISSUER = "autoservice-auth-test";

    @BeforeEach
    void setUp() {
        tokenGenerator = new JwtTokenGenerator(SECRET, ISSUER, 3600);
    }

    @Test
    void testGenerateToken() {
        // Act
        String token = tokenGenerator.generateToken("39053344705", "ATIVO");

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void testGeneratedTokenIsValid() {
        // Arrange
        String token = tokenGenerator.generateToken("39053344705", "ATIVO");

        // Act & Assert - Verificar se conseguimos decodificar
        var decoded = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        assertThat(decoded).isNotNull();
        assertThat(decoded.getBody().getSubject()).isEqualTo("39053344705");
    }

    @Test
    void testTokenContainsCorrectClaims() {
        // Arrange
        String token = tokenGenerator.generateToken("39053344705", "ATIVO");

        // Act
        var decoded = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        // Assert
        assertThat(decoded.getBody().getSubject()).isEqualTo("39053344705");
        assertThat(decoded.getBody().get("cpf")).isEqualTo("39053344705");
        assertThat(decoded.getBody().get("customer_status")).isEqualTo("ATIVO");
        assertThat(decoded.getBody().getIssuer()).isEqualTo(ISSUER);
        assertThat(decoded.getBody().get("roles")).isNotNull();
    }

    @Test
    void testTokenExpiration() {
        // Arrange
        String token = tokenGenerator.generateToken("39053344705", "ATIVO");

        // Act
        var decoded = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        long issuedAt = decoded.getBody().getIssuedAt().getTime();
        long expiresAt = decoded.getBody().getExpiration().getTime();
        long diffInSeconds = (expiresAt - issuedAt) / 1000;

        // Assert
        assertThat(diffInSeconds).isEqualTo(3600);
    }

    @Test
    void testMultipleTokensAreDifferent() throws InterruptedException {
        // Arrange & Act
        String token1 = tokenGenerator.generateToken("39053344705", "ATIVO");
        Thread.sleep(1100);
        String token2 = tokenGenerator.generateToken("39053344705", "ATIVO");

        // Assert - Tokens devem ser diferentes (timestamps diferentes)
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void testExpiresInSeconds() {
        assertThat(tokenGenerator.getExpiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void testRejectShortSecret() {
        assertThatThrownBy(() -> new JwtTokenGenerator("short-secret", ISSUER, 3600))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mínimo 32 bytes");
    }

    @Test
    void testRejectInvalidExpiration() {
        assertThatThrownBy(() -> new JwtTokenGenerator(SECRET, ISSUER, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void testTokenContainsKeyIdHeader() {
        String token = tokenGenerator.generateToken("39053344705", "ATIVO");

        var decoded = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        assertThat(decoded.getHeader().get("kid")).isEqualTo("test");
    }
}
