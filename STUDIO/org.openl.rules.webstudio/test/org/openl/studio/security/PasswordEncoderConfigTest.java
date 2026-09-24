package org.openl.studio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link PasswordEncoderConfig}.
 *
 * @author Yury Molchan
 */
class PasswordEncoderConfigTest {

    /** The lowest bcrypt cost keeps the tests fast. */
    private static final int STRENGTH = 4;

    private static PasswordEncoder encoder(String algorithm) {
        return new PasswordEncoderConfig().passwordEncoder(algorithm, STRENGTH);
    }

    @Test
    void bcryptHashesWithTheConfiguredStrength() {
        var encoder = encoder("bcrypt");

        var hash = encoder.encode("secret");

        assertTrue(hash.startsWith("$2a$04$"), hash);
        assertTrue(encoder.matches("secret", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void noopKeepsPasswordsInPlainText() {
        var encoder = encoder("noop");

        var hash = encoder.encode("secret");

        assertEquals("secret", hash);
        assertTrue(encoder.matches("secret", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void noopRejectsBcryptHashes() {
        var bcryptHash = encoder("bcrypt").encode("secret");

        assertFalse(encoder("noop").matches("secret", bcryptHash));
    }

    @Test
    void bcryptRejectsPlainTextHashes() {
        var plainTextHash = encoder("noop").encode("secret");

        assertFalse(encoder("bcrypt").matches("secret", plainTextHash));
    }

    @Test
    void otherAlgorithmsAreRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> encoder("argon2"));

        assertEquals("Unsupported security.password.encoder 'argon2'. Supported values: bcrypt, noop.",
                error.getMessage());
    }
}
