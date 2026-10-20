package com.bankpro.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContactNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {"01712345678", "+8801712345678", "8801712345678", "+880 1712-345678", "(017) 1234 5678"})
    void normalisesBangladeshMobileFormats(String raw) {
        assertThat(ContactNormalizer.normalizePhone(raw)).contains("+8801712345678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "01212345678", "0171234567", "017123456789", "abc", "+441712345678"})
    void rejectsInvalidNumbers(String raw) {
        assertThat(ContactNormalizer.normalizePhone(raw)).isEmpty();
    }

    @Test
    void normalisesEmailAndText() {
        assertThat(ContactNormalizer.normalizeEmail("  A.B@Example.COM ")).isEqualTo("a.b@example.com");
        assertThat(ContactNormalizer.normalizeEmail("   ")).isNull();
        assertThat(ContactNormalizer.clean("  Rahim    Uddin ")).isEqualTo("Rahim Uddin");
        assertThat(ContactNormalizer.clean("   ")).isNull();
    }
}
