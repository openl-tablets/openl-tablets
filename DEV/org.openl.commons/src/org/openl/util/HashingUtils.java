package org.openl.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashingUtils {

    private static final byte[] EMPTY_BYTES = new byte[0];

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
