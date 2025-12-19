package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class HashingUtilsTest {

    @Test
    void sha1_validString_returnsExpectedHash() {
        // "hello" in SHA-1 should produce a known hash
        String result = HashingUtils.sha1Hex("hello");
        assertNotNull(result);
        assertEquals(40, result.length(), "SHA-1 hash should be 20 bytes");
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", result);
    }

    @Test
    void sha1_emptyString_returnsExpectedHash() {
        String result = HashingUtils.sha1Hex("");
        assertNotNull(result);
        assertEquals(40, result.length());
        // SHA-1 of empty string
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", result);
    }

    @Test
    void sha1_nullString_returnsHashOfEmptyBytes() {
        byte[] result = HashingUtils.sha1(null);

        assertNotNull(result);
        assertEquals(20, result.length);

        // Should be same as empty string
        byte[] emptyResult = HashingUtils.sha1("");
        assertArrayEquals(emptyResult, result);
    }

    @Test
    void sha1_unicodeString_handlesCorrectly() {
        byte[] result = HashingUtils.sha1("Hello 世界 🌍");

        assertNotNull(result);
        assertEquals(20, result.length);

        // Verify it produces consistent results
        byte[] result2 = HashingUtils.sha1("Hello 世界 🌍");
        assertArrayEquals(result, result2, "Same input should produce same hash");
    }

    @Test
    void sha1_longString_handlesCorrectly() {
        String longString = "a".repeat(10000);
        byte[] result = HashingUtils.sha1(longString);

        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    void sha256Hex_validString_returnsExpectedHash() {
        // "hello" in SHA-256 should produce a known hash
        String result = HashingUtils.sha256Hex("hello");

        assertNotNull(result);
        assertEquals(64, result.length(), "SHA-256 hex string should be 64 characters");
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result);
    }

    @Test
    void sha256Hex_emptyString_returnsExpectedHash() {
        String result = HashingUtils.sha256Hex("");

        assertNotNull(result);
        assertEquals(64, result.length());
        // SHA-256 of empty string
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result);
    }

    @Test
    void sha256Hex_nullString_returnsHashOfEmptyBytes() {
        String result = HashingUtils.sha256Hex(null);

        assertNotNull(result);
        assertEquals(64, result.length());

        // Should be same as empty string
        String emptyResult = HashingUtils.sha256Hex("");
        assertEquals(emptyResult, result);
    }

    @Test
    void sha256Hex_unicodeString_handlesCorrectly() {
        String result = HashingUtils.sha256Hex("Hello 世界 🌍");

        assertNotNull(result);
        assertEquals(64, result.length());

        // Verify it produces consistent results
        String result2 = HashingUtils.sha256Hex("Hello 世界 🌍");
        assertEquals(result, result2, "Same input should produce same hash");
    }

    @Test
    void sha256Hex_longString_handlesCorrectly() {
        String longString = "a".repeat(10000);
        String result = HashingUtils.sha256Hex(longString);

        assertNotNull(result);
        assertEquals(64, result.length());
    }

    @Test
    void sha256Hex_specialCharacters_handlesCorrectly() {
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
        String result = HashingUtils.sha256Hex(specialChars);

        assertNotNull(result);
        assertEquals(64, result.length());
    }

    @Test
    void sha256Hex_returnsLowercaseHex() {
        String result = HashingUtils.sha256Hex("test");

        // Verify all characters are lowercase hex
        assertTrue(result.matches("^[0-9a-f]{64}$"), "Hash should be lowercase hex");
    }

    @Test
    void sha1_deterministicResults_sameInputProducesSameOutput() {
        String input = "test data";

        byte[] result1 = HashingUtils.sha1(input);
        byte[] result2 = HashingUtils.sha1(input);
        byte[] result3 = HashingUtils.sha1(input);

        assertArrayEquals(result1, result2);
        assertArrayEquals(result2, result3);
    }

    @Test
    void sha256Hex_deterministicResults_sameInputProducesSameOutput() {
        String input = "test data";

        String result1 = HashingUtils.sha256Hex(input);
        String result2 = HashingUtils.sha256Hex(input);
        String result3 = HashingUtils.sha256Hex(input);

        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    @Test
    void sha1_differentInputs_produceDifferentHashes() {
        byte[] hash1 = HashingUtils.sha1("input1");
        byte[] hash2 = HashingUtils.sha1("input2");

        assertFalse(Arrays.equals(hash1, hash2), "Different inputs should produce different hashes");
    }

    @Test
    void sha256Hex_differentInputs_produceDifferentHashes() {
        String hash1 = HashingUtils.sha256Hex("input1");
        String hash2 = HashingUtils.sha256Hex("input2");

        assertNotEquals(hash1, hash2, "Different inputs should produce different hashes");
    }
}
