package org.openl.rules.repository.config;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.openl.util.StringUtils;

/**
 * @author Pavel Tarasevich
 *
 */
public final class PassCoder {
    private static byte[] bytes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static IvParameterSpec algorithmParameterSpec = new IvParameterSpec(bytes);
    private static String encoding = "UTF-8";

    private PassCoder() {
    }

    public static String encode(String strToEncrypt, String privateKey) throws NoSuchAlgorithmException,
                                                                        NoSuchPaddingException,
                                                                        InvalidKeyException,
                                                                        IllegalBlockSizeException,
                                                                        BadPaddingException,
                                                                        UnsupportedEncodingException,
                                                                        InvalidAlgorithmParameterException {
        if (StringUtils.isBlank(strToEncrypt)) {
            return "";
        }
        if (StringUtils.isBlank(privateKey)) {
            return strToEncrypt;
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        SecretKeySpec secretKey = getKey(privateKey);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, algorithmParameterSpec);
        return new String(Base64.encodeBase64(cipher.doFinal(strToEncrypt.getBytes(encoding))));
    }

    public static String decode(String strToDecrypt, String privateKey) throws NoSuchAlgorithmException,
                                                                        NoSuchPaddingException,
                                                                        InvalidKeyException,
                                                                        IllegalBlockSizeException,
                                                                        BadPaddingException,
                                                                        UnsupportedEncodingException,
                                                                        InvalidAlgorithmParameterException {
        if (StringUtils.isBlank(strToDecrypt)) {
            return "";
        }
        if (StringUtils.isBlank(privateKey)) {
            return strToDecrypt;
        }

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = getKey(privateKey);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, algorithmParameterSpec);
        return new String(cipher.doFinal(Base64.decodeBase64(strToDecrypt.getBytes(encoding))));
    }

    private static SecretKeySpec getKey(String privateKey) throws UnsupportedEncodingException,
                                                           NoSuchAlgorithmException {
        byte[] key = privateKey.getBytes(encoding);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit

        return new SecretKeySpec(key, "AES");
    }
}
