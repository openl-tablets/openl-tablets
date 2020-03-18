package org.openl.rules.webstudio.security;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class KeyStoreUtils {

    /**
     * save the keystore to disk.
     *
     * @param keystore file to save the keystore.
     * @param ks the keystore object.
     * @param keystorePassword the password to set to the keystore.
     * @throws KeyStoreException @see KeyStoreException
     * @throws IOException @see IOException
     * @throws NoSuchAlgorithmException @see NoSuchAlgorithmException
     * @throws CertificateException @see CertificateException
     */
    public static void saveKeyStore(File keystore, KeyStore ks, String keystorePassword) throws IOException,
                                                                                         CertificateException,
                                                                                         NoSuchAlgorithmException,
                                                                                         KeyStoreException {
        try (FileOutputStream fos = new FileOutputStream(keystore)) {
            ks.store(fos, keystorePassword.toCharArray());
        }
    }

    /**
     * load a keystore from a file. if it fails throws an exception.
     *
     * @param keystore path to the keystore.
     * @param password password of the keystore.
     * @return the keystore loaded.
     * @throws KeyStoreException if keystore is invalid
     * @throws CertificateException if certificate is invalid
     * @throws NoSuchAlgorithmException @see NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore(File keystore,
            String password) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(keystore)) {
            ks.load(in, password.toCharArray());
            return ks;
        }

    }

    /**
     *
     * @param certificate - PEM-formatted String
     * @return X509 certificate instance
     * @throws CertificateException if certificate is invalid
     */
    public static X509Certificate generateCertificate(String certificate) throws CertificateException {
        byte[] decoded = Base64.getDecoder().decode(certificate);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
    }

    public static boolean isCertificateValid(X509Certificate certificate) {
        boolean result = true;
        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            result = false;
        }
        return result;
    }
}
