package com.autoservice.lambda.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes para validação de CPF.
 */
class CpfValidatorTest {

    @Test
    void testOnlyDigits() {
        assertThat(CpfValidator.onlyDigits("390.533.447-05")).isEqualTo("39053344705");
        assertThat(CpfValidator.onlyDigits("111.444.777-35")).isEqualTo("11144477735");
        assertThat(CpfValidator.onlyDigits("")).isEqualTo("");
        assertThat(CpfValidator.onlyDigits(null)).isEqualTo("");
    }

    @Test
    void testFormatCpf() {
        assertThat(CpfValidator.format("39053344705")).isEqualTo("390.533.447-05");
        assertThat(CpfValidator.format("11144477735")).isEqualTo("111.444.777-35");
    }

    @Test
    void testValidCpf() {
        assertThat(CpfValidator.isValid("390.533.447-05")).isTrue();
        assertThat(CpfValidator.isValid("39053344705")).isTrue();
        assertThat(CpfValidator.isValid("111.444.777-35")).isTrue();
        assertThat(CpfValidator.isValid("11144477735")).isTrue();
    }

    @Test
    void testInvalidCpfAllSameDigits() {
        assertThat(CpfValidator.isValid("11111111111")).isFalse();
        assertThat(CpfValidator.isValid("22222222222")).isFalse();
        assertThat(CpfValidator.isValid("00000000000")).isFalse();
    }

    @Test
    void testInvalidCpfWrongCheckDigit() {
        assertThat(CpfValidator.isValid("39053344706")).isFalse();
        assertThat(CpfValidator.isValid("39053344704")).isFalse();
    }

    @Test
    void testInvalidCpfTooShort() {
        assertThat(CpfValidator.isValid("123")).isFalse();
        assertThat(CpfValidator.isValid("12345678901")).isFalse();
    }
}
