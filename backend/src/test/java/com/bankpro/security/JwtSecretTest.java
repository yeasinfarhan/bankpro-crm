package com.bankpro.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretTest {

    @Test
    @DisplayName("JWT_SECRET may be standard or URL-safe Base64; anything else is refused at start-up")
    void decodesBothAlphabets() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) (0xF0 + i); // produces '+', '/' and '-', '_' in the encodings
        assertThat(JwtConfig.decodeSecret(Base64.getEncoder().encodeToString(key))).isEqualTo(key);
        assertThat(JwtConfig.decodeSecret(Base64.getUrlEncoder().encodeToString(key))).isEqualTo(key);
        assertThatThrownBy(() -> JwtConfig.decodeSecret("not base64 at all!"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Base64");
    }
}
