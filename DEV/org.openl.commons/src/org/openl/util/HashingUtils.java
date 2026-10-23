package org.openl.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashingUtils {

    private static final byte[] EMPTY_BYTES = new byte[0];
    /** Crockford's Base32 alphabet: the digits and the upper-case letters except I, L, O and U. */
    private static final String BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int BASE32_BITS = 5;

    private HashingUtils() {

    }

    private static MessageDigest getSha1Digest() {
        return getDigest("SHA-1");
    }

    public static byte[] sha1(String data) {
        return getSha1Digest().digest(getBytesUtf8(data));
    }

    public static String sha1Hex(String data) {
        return encodeHexString(sha1(data));
    }

    private static MessageDigest getSha256Digest() {
        return getDigest("SHA-256");
    }

    public static byte[] sha256(String data) {
        return getSha256Digest().digest(getBytesUtf8(data));
    }

    public static String sha256Hex(String data) {
        return encodeHexString(sha256(data));
    }

    /**
     * The leading bits of the SHA-256 digest of the text, written in Crockford's Base32.
     *
     * <p>Each symbol writes 5 bits: a digit or an upper-case letter other than {@code I}, {@code L}, {@code O} and
     * {@code U}. The text has no padding, so {@code 13} symbols write the first 65 bits of the digest.
     *
     * @param data    the text to digest; {@code null} is digested as an empty text
     * @param symbols how many symbols to write, from 1 to 51
     * @return the symbols
     * @throws IllegalArgumentException when the digest has too few bits for the symbols asked
     */
    public static String sha256Base32(String data, int symbols) {
        var digest = sha256(data);
        if (symbols < 1 || symbols > digest.length * Byte.SIZE / BASE32_BITS) {
            throw new IllegalArgumentException("Symbols must be from 1 to 51, but was " + symbols);
        }
        var text = new char[symbols];
        for (var i = 0; i < symbols; i++) {
            var bit = i * BASE32_BITS;
            var index = bit / Byte.SIZE;
            var next = index + 1 < digest.length ? digest[index + 1] & 0xFF : 0;
            var twoBytes = (digest[index] & 0xFF) << Byte.SIZE | next;
            text[i] = BASE32.charAt(twoBytes >>> (Short.SIZE - BASE32_BITS - bit % Byte.SIZE) & 0x1F);
        }
        return new String(text);
    }

    private static String encodeHexString(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private static byte[] getBytesUtf8(String data) {
        return data == null ? EMPTY_BYTES : data.getBytes(StandardCharsets.UTF_8);
    }

    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
