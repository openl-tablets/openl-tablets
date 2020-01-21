package org.openl.spring.env;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.openl.util.StringUtils;

/**
 * @author Pavel Tarasevich
 *
 */
public final class PassCoder {
    private static byte[] bytes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static IvParameterSpec algorithmParameterSpec = new IvParameterSpec(bytes);

    private PassCoder() {
    }

    public static String encode(String strToEncrypt, String privateKey) throws NoSuchAlgorithmException,
                                                                        NoSuchPaddingException,
                                                                        InvalidKeyException,
                                                                        IllegalBlockSizeException,
                                                                        BadPaddingException,
                                                                        InvalidAlgorithmParameterException {
        if (StringUtils.isBlank(strToEncrypt)) {
            return strToEncrypt;
        }
        if (StringUtils.isBlank(privateKey)) {
            return strToEncrypt;
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        SecretKeySpec secretKey = getKey(privateKey);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, algorithmParameterSpec);
        byte[] toEncrypt = strToEncrypt.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(toEncrypt);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decode(String strToDecrypt, String privateKey) throws NoSuchAlgorithmException,
                                                                        NoSuchPaddingException,
                                                                        InvalidKeyException,
                                                                        IllegalBlockSizeException,
                                                                        BadPaddingException,
                                                                        InvalidAlgorithmParameterException {
        if (StringUtils.isBlank(strToDecrypt)) {
            return strToDecrypt;
        }
        if (StringUtils.isBlank(privateKey)) {
            return strToDecrypt;
        }

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = getKey(privateKey);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, algorithmParameterSpec);
        byte[] toDecrypt = Base64.getDecoder().decode(strToDecrypt);
        byte[] decripted = cipher.doFinal(toDecrypt);
        return new String(decripted, StandardCharsets.UTF_8);
    }

    private static SecretKeySpec getKey(String privateKey) throws NoSuchAlgorithmException {
        byte[] key = privateKey.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit

        return new SecretKeySpec(key, "AES");
    }
}
