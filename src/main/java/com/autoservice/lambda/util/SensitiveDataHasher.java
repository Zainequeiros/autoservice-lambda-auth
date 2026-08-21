package com.autoservice.lambda.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitário para hash de dados sensíveis em logs.
 */
public class SensitiveDataHasher {

    private SensitiveDataHasher() {
        // Utility class
    }

    public static String sha256Short(String value) {
        if (value == null || value.isBlank()) {
            return "na";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
