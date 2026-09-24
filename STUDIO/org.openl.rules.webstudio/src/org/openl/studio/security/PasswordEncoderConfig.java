package org.openl.studio.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hashing of the passwords of internal users and of the secrets of personal access tokens.
 *
 * @author Yury Molchan
 */
@Slf4j
@Configuration
class PasswordEncoderConfig {

    private static final String BCRYPT = "bcrypt";
    private static final String NOOP = "noop";

    /**
     * Creates the encoder that hashes passwords and token secrets.
     * <p>
     * {@code security.password.encoder} selects the algorithm: {@code bcrypt}, or {@code noop} that keeps the value
     * in plain text for integration tests and logs a warning.
     * <p>
     * Each algorithm verifies only its own hashes, so the values stored under one algorithm do not match after a
     * switch to the other.
     *
     * @param algorithm the algorithm
     * @param strength  the bcrypt cost, from 4 to 31
     * @return the encoder
     * @throws IllegalArgumentException if the algorithm is neither {@code bcrypt} nor {@code noop}
     */
    @Bean
    PasswordEncoder passwordEncoder(@Value("${security.password.encoder}") String algorithm,
                                    @Value("${webstudio.bcrypt.strength}") int strength) {
        return switch (algorithm) {
            case BCRYPT -> new BCryptPasswordEncoder(strength);
            case NOOP -> plainText();
            default -> throw new IllegalArgumentException(
                    "Unsupported security.password.encoder '%s'. Supported values: %s, %s."
                            .formatted(algorithm, BCRYPT, NOOP));
        };
    }

    /**
     * Keeps passwords and token secrets in plain text, and logs a warning that it does.
     *
     * <p>Only integration tests select it: it spares every request they authenticate the cost of a bcrypt check.
     */
    // Storing plain text is the purpose here, and Spring deprecates the class only to mark it insecure.
    @SuppressWarnings({"deprecation", "java:S4790", "java:S5344"})
    private static PasswordEncoder plainText() {
        log.warn("security.password.encoder = noop stores passwords and token secrets in plain text."
                + " Use it only in tests.");
        return NoOpPasswordEncoder.getInstance();
    }
}
