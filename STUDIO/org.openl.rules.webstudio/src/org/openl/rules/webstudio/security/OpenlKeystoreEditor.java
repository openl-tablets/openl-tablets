package org.openl.rules.webstudio.security;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

public class OpenlKeystoreEditor {
    private Resource keyStoreFile;
    private String keystorePassword;
    private String serverCertificate;
    private String serverKeyAlias;

    private final Logger log = LoggerFactory.getLogger(OpenlKeystoreEditor.class);

    public OpenlKeystoreEditor(Resource keyStoreFile,
            String keystorePassword,
            String serverCertificate,
            String serverKeyAlias) {
        this.keyStoreFile = keyStoreFile;
        this.keystorePassword = keystorePassword;
        this.serverCertificate = serverCertificate;
        this.serverKeyAlias = serverKeyAlias;
        modifyKeystore();
    }

    private void modifyKeystore() {
        if (StringUtils.isBlank(serverCertificate)) {
            return;
        }
        if (StringUtils.isBlank(serverKeyAlias)) {
            throw new IllegalStateException("Server key alias is empty, but key was defined");
        }

        File keystore = null;
        KeyStore ks = null;
        try {
            keystore = keyStoreFile.getFile();
            ks = KeyStoreUtils.loadKeyStore(keystore, keystorePassword);
            // if there is already a valid certificate inside no need to rewrite it
            if (ks.isCertificateEntry(serverKeyAlias)) {
                X509Certificate certificate = (X509Certificate) ks.getCertificate(serverKeyAlias);
                if (KeyStoreUtils.isCertificateValid(certificate)) {
                    return;
                }
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            log.error("Failed to read keystore", e);
        }

        X509Certificate cert = null;
        try {
            cert = KeyStoreUtils.generateCertificate(serverCertificate);
            cert.checkValidity();
        } catch (CertificateException e) {
            log.error("Failed to generate certificate", e);
        }

        if (ks != null && cert != null) {
            try {
                ks.setCertificateEntry(serverKeyAlias, cert);
                KeyStoreUtils.saveKeyStore(keystore, ks, keystorePassword);
            } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
                log.error("Failed to save keystore", e);
            }
        }
    }

}
