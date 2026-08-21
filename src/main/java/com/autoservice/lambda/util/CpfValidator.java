package com.autoservice.lambda.util;

import java.util.regex.Pattern;

/**
 * Utilitários para validação de CPF.
 */
public class CpfValidator {

    private static final Pattern DIGITS_ONLY = Pattern.compile("[\\D]");

    private CpfValidator() {
        // Utility class
    }

    /**
     * Remove tudo exceto dígitos.
     */
    public static String onlyDigits(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return DIGITS_ONLY.matcher(value).replaceAll("");
    }

    /**
     * Valida um CPF de acordo com as regras do Brasil.
     *
     * @param cpf CPF formatado ou não (com ou sem pontos/hífens)
     * @return true se válido, false caso contrário
     */
    public static boolean isValid(String cpf) {
        String digits = onlyDigits(cpf);

        // Deve ter exatamente 11 dígitos
        if (digits.length() != 11) {
            return false;
        }

        // Não pode ser todos o mesmo dígito
        if (digits.matches("^(\\d)\\1{10}$")) {
            return false;
        }

        // Valida primeiro dígito verificador
        int firstSum = 0;
        for (int i = 0; i < 9; i++) {
            firstSum += Character.getNumericValue(digits.charAt(i)) * (10 - i);
        }
        int firstCheck = (firstSum * 10 % 11) % 10;
        if (firstCheck != Character.getNumericValue(digits.charAt(9))) {
            return false;
        }

        // Valida segundo dígito verificador
        int secondSum = 0;
        for (int i = 0; i < 10; i++) {
            secondSum += Character.getNumericValue(digits.charAt(i)) * (11 - i);
        }
        int secondCheck = (secondSum * 10 % 11) % 10;

        return secondCheck == Character.getNumericValue(digits.charAt(10));
    }

    /**
     * Formata CPF para XXX.XXX.XXX-XX.
     *
     * @param cpf CPF com apenas dígitos
     * @return CPF formatado
     */
    public static String format(String cpf) {
        String digits = onlyDigits(cpf);
        if (digits.length() != 11) {
            return digits;
        }
        return String.format("%s.%s.%s-%s",
                digits.substring(0, 3),
                digits.substring(3, 6),
                digits.substring(6, 9),
                digits.substring(9));
    }
}
