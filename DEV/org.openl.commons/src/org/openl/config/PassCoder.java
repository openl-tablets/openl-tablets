package org.openl.config;

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

/**
 * @author Pavel Tarasevich
 * 
 */

class PassCoder {
    static byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    static IvParameterSpec ivspec = new IvParameterSpec(iv);
    static private String encoding = "UTF-8";

    static String encode(String strToEncrypt, String privateKey) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            UnsupportedEncodingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        SecretKeySpec secretKey = getKey(privateKey);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
        return new String(Base64.encodeBase64(cipher.doFinal(strToEncrypt.getBytes(encoding))));
    }

    static String decode(String strToDecrypt, String privateKey) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            UnsupportedEncodingException, InvalidAlgorithmParameterException {
        byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        IvParameterSpec ivspec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = getKey(privateKey);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
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
