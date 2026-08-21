package com.autoservice.lambda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de response para autenticação.
 */
public class AuthResponse {
    @JsonProperty("token")
    private String token;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn;

    // Constructors
    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, long expiresIn) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
