package org.openl.studio.audit.service;

import java.security.SecureRandom;
import java.util.HexFormat;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.studio.settings.converter.SecretSettings;
import org.openl.util.HashingUtils;

/**
 * Fingerprints of the values of settings: a fingerprint tells whether a value is the one it was made of, and never
 * gives the value back.
 *
 * <p>The value of an ordinary setting is fingerprinted by SHA-256 with a salt of its own, written as
 * {@code sha256$<salt>$<hash>}. The value of a secret setting goes through the given password encoder, such as
 * bcrypt, written as {@code encoded$<hash>}: guessing it takes as long as guessing a password. The encoder gets the
 * SHA-256 of the value, so a value longer than the encoder takes is fingerprinted whole.
 *
 * <p>A fingerprint is read by the scheme it names, so a setting that became secret still matches its fingerprint;
 * it is then fingerprinted anew by its new scheme.
 */
@RequiredArgsConstructor
final class SettingFingerprints {

    private static final String PLAIN = "sha256$";
    private static final String ENCODED = "encoded$";
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final PasswordEncoder passwordEncoder;

    /**
     * Makes the fingerprint of the value of a setting.
     */
    String of(String property, String value) {
        if (SecretSettings.isSecret(property)) {
            return ENCODED + passwordEncoder.encode(HashingUtils.sha256Hex(value));
        }
        var salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        var saltText = HEX.formatHex(salt);
        return PLAIN + saltText + "$" + HashingUtils.sha256Hex(saltText + value);
    }

    /**
     * Whether the fingerprint was made of the value. A fingerprint that cannot be read matches no value.
     */
    boolean matches(String value, String fingerprint) {
        if (fingerprint.startsWith(ENCODED)) {
            return passwordEncoder.matches(HashingUtils.sha256Hex(value), fingerprint.substring(ENCODED.length()));
        }
        var parts = fingerprint.split("\\$");
        return fingerprint.startsWith(PLAIN) && parts.length == 3
                && HashingUtils.sha256Hex(parts[1] + value).equals(parts[2]);
    }

    /**
     * Whether the fingerprint of a setting is made the way a new one would be: a secret setting by the password
     * encoder, any other one by SHA-256.
     */
    boolean isCurrent(String property, String fingerprint) {
        return fingerprint.startsWith(SecretSettings.isSecret(property) ? ENCODED : PLAIN);
    }
}
