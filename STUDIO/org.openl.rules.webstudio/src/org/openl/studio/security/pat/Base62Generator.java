package org.openl.studio.security.pat;

import java.security.SecureRandom;

/**
 * Utility class for generating cryptographically secure random Base62-encoded strings.
 * <p>
 * Base62 encoding uses alphanumeric characters (0-9, a-z, A-Z), which makes the generated
 * strings URL-safe and suitable for use as public identifiers and secrets in Personal Access Tokens.
 * </p>
 * <p>
 * This class uses {@link SecureRandom} to ensure cryptographic strength of generated strings.
 * </p>
 *
 * @since 6.0.0
 */
public final class Base62Generator {

    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final static SecureRandom SECURE_RANDOM = new SecureRandom();

    private Base62Generator() {
    }

    /**
     * Generates a cryptographically secure random Base62 string of the specified length.
     *
     * @param length the desired length of the generated string (must be greater than 0)
     * @return a random Base62-encoded string
     * @throws IllegalArgumentException if length is less than 1
     */
    public static String generate(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be > 0");
        }

        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = ALPHABET[SECURE_RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }

}
