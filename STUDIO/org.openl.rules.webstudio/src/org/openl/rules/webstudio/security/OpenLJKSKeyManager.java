package org.openl.rules.webstudio.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.security.saml.key.JKSKeyManager;

/**
 * Needed to work with user certificates without modifying the keystore file.
 *
 * @author Eugene Biruk
 */
public class OpenLJKSKeyManager extends JKSKeyManager {

    private final Map<String, String> availableCredentials;

    public OpenLJKSKeyManager(Resource storeFile, String storePass, Map<String, String> passwords, String defaultKey,
                              Map<String, String> availableCredentials) {
        super(storeFile, storePass, passwords, defaultKey);
        this.availableCredentials = availableCredentials;
    }

    @Override
    public X509Certificate getCertificate(String alias) {
        X509Certificate certificate = super.getCertificate(alias);
        if (certificate == null && availableCredentials.containsKey(alias)) {
            String stringCertificate = availableCredentials.get(alias);
            if (stringCertificate != null) {
                try {
                    certificate = KeyStoreUtils.generateCertificate(stringCertificate);
                } catch (CertificateException e) {
                    throw new IllegalStateException("The certificate isn't valid", e);
                }
            }
        }
        return certificate;
    }

    @Override
    public Set<String> getAvailableCredentials() {
        Set<String> credentials = super.getAvailableCredentials();
        credentials.addAll(availableCredentials.keySet());
        return credentials;
    }
}
