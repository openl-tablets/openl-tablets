package org.openl.security.saml;

import org.openl.util.StringUtils;
import org.opensaml.security.x509.X509Support;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Iterator;

/**
 * Lazy RelyingPartyRegistration initialization, for the case when IDP is not available for some reason.
 *
 * @author Eugene Biruk
 */
public class LazyInMemoryRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {

    private static final Logger log = LoggerFactory.getLogger(LazyInMemoryRelyingPartyRegistrationRepository.class);

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private PropertyResolver propertyResolver;

    public LazyInMemoryRelyingPartyRegistrationRepository(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        init();
    }

    private void init() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(propertyResolver.getProperty("security.saml.local-key")));
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(propertyResolver.getProperty("security.saml.local-certificate")));
            X509Certificate localCert = (X509Certificate) certFactory.generateCertificate(in);

            Saml2X509Credential signing = Saml2X509Credential.signing(privateKey, localCert);
            Saml2X509Credential decryption = Saml2X509Credential.decryption(privateKey, localCert);
            RelyingPartyRegistration.Builder registrationBuilder = RelyingPartyRegistrations
                .fromMetadataLocation(propertyResolver.getProperty("security.saml.saml-server-metadata-url"))
                .registrationId("webstudio")
                .entityId(propertyResolver.getProperty("security.saml.entity-id"))
                .signingX509Credentials(c -> c.add(signing))
                .decryptionX509Credentials(c -> c.add(decryption));

            // Override certificate from the Metadata XML to prevent MITM attack.
            String serverCertificate = propertyResolver.getProperty("security.saml.server-certificate");
            if (StringUtils.isNotBlank(serverCertificate)) {
                X509Certificate idpCert = X509Support.decodeCertificate(serverCertificate);
                Saml2X509Credential verification = Saml2X509Credential.verification(idpCert);
                registrationBuilder.assertingPartyDetails(party -> party
                        .verificationX509Credentials(c -> {
                            c.clear();
                            c.add(verification);
                        })
                );
            }

            RelyingPartyRegistration registration = registrationBuilder.build();

            relyingPartyRegistrationRepository = new InMemoryRelyingPartyRegistrationRepository(registration);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Returns the relying party registration identified by the provided.
     * Initializes RelyingPartyRegistration if it has not been created yet.
     * @param id the registration identifier
     * @return the {@link RelyingPartyRegistration} if found, otherwise {@code null}
     */
    @Override
    public RelyingPartyRegistration findByRegistrationId(String id) {
        if (relyingPartyRegistrationRepository == null) {
            init();
        }
        return relyingPartyRegistrationRepository != null ? relyingPartyRegistrationRepository.findByRegistrationId(id) : null;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     * Initializes RelyingPartyRegistration if it has not been created yet.
     * @return an Iterator.
     */
    @Override
    public Iterator<RelyingPartyRegistration> iterator() {
        if (relyingPartyRegistrationRepository == null) {
            init();
        }
        return relyingPartyRegistrationRepository != null ? relyingPartyRegistrationRepository.iterator() : null;
    }
}
