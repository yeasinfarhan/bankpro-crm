package com.bankpro.security;

import com.bankpro.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates the signing key and the JWT encoder/decoder.
 * <p>
 * We use HS256: the same secret signs and verifies tokens, which is simple and fine for a single backend.
 * The secret comes from the JWT_SECRET environment variable. In the dev profile only, if it is missing,
 * a random key is generated at startup (everyone is logged out on restart) so the app still runs.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int MIN_KEY_BYTES = 32; // 256 bits, required for HS256

    @Bean
    public SecretKey jwtSigningKey(AppProperties props) {
        AppProperties.Jwt jwt = props.jwt();
        byte[] keyBytes;
        if (jwt.secret() == null || jwt.secret().isBlank()) {
            if (!jwt.allowEphemeralSecret()) {
                throw new IllegalStateException("JWT_SECRET is not set. Generate one with: openssl rand -base64 48");
            }
            keyBytes = new byte[48];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("JWT_SECRET not set: using a random key for this run (dev only). Tokens become invalid on restart.");
        } else {
            keyBytes = decodeSecret(jwt.secret().trim());
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET must decode to at least " + MIN_KEY_BYTES + " bytes");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /** Accepts standard or URL-safe Base64 (hosting platforms generate either). */
    static byte[] decodeSecret(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException standard) {
            try {
                return Base64.getUrlDecoder().decode(secret);
            } catch (IllegalArgumentException urlSafe) {
                throw new IllegalStateException("JWT_SECRET must be Base64-encoded (openssl rand -base64 48)", standard);
            }
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, AppProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Checks signature (above), expiry/not-before (default validators) and that we issued it.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.jwt().issuer()));
        return decoder;
    }
}
