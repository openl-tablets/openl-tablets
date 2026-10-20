package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SettingFingerprintsTest {

    private final SettingFingerprints fingerprints = new SettingFingerprints(new BCryptPasswordEncoder(4));

    @Test
    void anOrdinarySettingIsFingerprintedBySha256WithASaltOfItsOwn() {
        var fingerprint = fingerprints.of("mail.url", "smtp://mail.example.com");

        assertTrue(fingerprint.startsWith("sha256$"));
        assertTrue(fingerprints.matches("smtp://mail.example.com", fingerprint));
        assertFalse(fingerprints.matches("smtp://other.example.com", fingerprint));
        assertFalse(fingerprint.contains("example.com"), "A fingerprint gives no value back");
        assertNotEquals(fingerprint, fingerprints.of("mail.url", "smtp://mail.example.com"));
    }

    @Test
    void aSecretSettingIsFingerprintedByThePasswordEncoder() {
        var fingerprint = fingerprints.of("repository.design.password", "s3cret-Пароль");

        assertTrue(fingerprint.startsWith("encoded$$2a$"), "The password encoder makes it");
        assertTrue(fingerprints.matches("s3cret-Пароль", fingerprint));
        assertFalse(fingerprints.matches("s3cret-пароль", fingerprint));
        assertFalse(fingerprint.contains("s3cret"));
    }

    @Test
    void aSecretLongerThanTheEncoderTakesIsFingerprintedWhole() {
        var key = "k".repeat(100);
        var fingerprint = fingerprints.of("repository.production.account-key", key);

        assertTrue(fingerprints.matches(key, fingerprint));
        assertFalse(fingerprints.matches("k".repeat(99) + "x", fingerprint), "The tail beyond 72 bytes counts too");
    }

    @Test
    void aFingerprintThatCannotBeReadMatchesNoValue() {
        assertFalse(fingerprints.matches("multi", "multi"));
        assertFalse(fingerprints.matches("multi", "sha256$salt"));
        assertFalse(fingerprints.matches("multi", "md5$salt$hash"));
        assertFalse(fingerprints.matches("multi", "encoded$not-bcrypt"));
    }

    @Test
    void tellsAFingerprintMadeTheWayANewOneWouldBe() {
        var plain = fingerprints.of("user.mode", "multi");

        assertTrue(fingerprints.isCurrent("user.mode", plain));
        assertFalse(fingerprints.isCurrent("mail.password", plain), "A setting that became secret");
        assertTrue(fingerprints.isCurrent("mail.password", fingerprints.of("mail.password", "s3cret")));
    }
}
