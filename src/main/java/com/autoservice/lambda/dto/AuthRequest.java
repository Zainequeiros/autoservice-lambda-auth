package com.autoservice.lambda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de request para autenticação.
 */
public class AuthRequest {
    @JsonProperty("cpf")
    private String cpf;

    // Constructors
    public AuthRequest() {
    }

    public AuthRequest(String cpf) {
        this.cpf = cpf;
    }

    // Getters and Setters
    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
}
