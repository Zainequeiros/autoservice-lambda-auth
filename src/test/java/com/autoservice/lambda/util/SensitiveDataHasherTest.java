package com.autoservice.lambda.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataHasherTest {

    @Test
    void testSha256Short() {
        String hash1 = SensitiveDataHasher.sha256Short("39053344705");
        String hash2 = SensitiveDataHasher.sha256Short("39053344705");
        String hash3 = SensitiveDataHasher.sha256Short("11144477735");

        assertThat(hash1).hasSize(16);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
    }

    @Test
    void testSha256ShortWithNullOrBlank() {
        assertThat(SensitiveDataHasher.sha256Short(null)).isEqualTo("na");
        assertThat(SensitiveDataHasher.sha256Short(" ")).isEqualTo("na");
    }
}
