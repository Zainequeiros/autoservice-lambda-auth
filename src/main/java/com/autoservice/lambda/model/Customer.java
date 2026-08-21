package com.autoservice.lambda.model;

/**
 * Model de cliente.
 */
public class Customer {
    private String cpf;
    private String status;
    private boolean active;

    // Constructors
    public Customer() {
    }

    public Customer(String cpf, String status, boolean active) {
        this.cpf = cpf;
        this.status = status;
        this.active = active;
    }

    // Getters and Setters
    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "cpf='" + cpf + '\'' +
                ", status='" + status + '\'' +
                ", active=" + active +
                '}';
    }
}
