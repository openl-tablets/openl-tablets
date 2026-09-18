package org.openl.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link AllowedOrigins}.
 *
 * @author Yury Molchan
 */
class AllowedOriginsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", " , "})
    void blankSettingTrustsNothing(String value) {
        var origins = AllowedOrigins.parse(value);

        assertFalse(origins.contains("https://app.example.com"));
        assertFalse(origins.any());
    }

    @Test
    void asteriskTrustsEveryOriginButNotAMissingOne() {
        var origins = AllowedOrigins.parse(" * ");

        assertTrue(origins.contains("https://evil.example"));
        assertFalse(origins.contains(null));
    }

    @Test
    void listedOriginMatchesIgnoringCaseAndSpaces() {
        var origins = AllowedOrigins.parse("https://app.example.com , HTTPS://Other.Example");

        assertTrue(origins.contains("HTTPS://APP.example.com"));
        assertTrue(origins.contains("https://other.example"));
    }

    @Test
    void unlistedOriginIsNotTrusted() {
        var origins = AllowedOrigins.parse("https://app.example.com");

        assertFalse(origins.contains("https://app.example.com:8443"));
        assertFalse(origins.contains("https://evil.example"));
        assertFalse(origins.contains(null));
    }
}
