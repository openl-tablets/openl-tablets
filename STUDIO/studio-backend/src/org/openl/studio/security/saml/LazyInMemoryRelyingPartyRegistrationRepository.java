package org.openl.studio.security.saml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;
import org.opensaml.security.x509.X509Support;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.AssertingPartyMetadata;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;


/**
 * Lazy RelyingPartyRegistration initialization, for the case when IDP is not available for some reason.
 *
 * @author Eugene Biruk
 */
@Slf4j
public class LazyInMemoryRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);


    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private PropertyResolver propertyResolver;

    public LazyInMemoryRelyingPartyRegistrationRepository(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        init();
    }

    private void init() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            var privateKeySpec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(propertyResolver.getProperty("security.saml.local-key")));
            var privateKey = keyFactory.generatePrivate(privateKeySpec);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            var in = new ByteArrayInputStream(Base64.getMimeDecoder().decode(propertyResolver.getProperty("security.saml.local-certificate")));
            var localCert = (X509Certificate) certFactory.generateCertificate(in);

            Saml2X509Credential signing = Saml2X509Credential.signing(privateKey, localCert);
            Saml2X509Credential decryption = Saml2X509Credential.decryption(privateKey, localCert);
            var registrationBuilder = fromMetadata(propertyResolver.getProperty("security.saml.saml-server-metadata-url"),
                    CONNECT_TIMEOUT,
                    READ_TIMEOUT)
                    .registrationId("webstudio")
                    .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo")
                    .entityId(propertyResolver.getProperty("security.saml.entity-id"))
                    .signingX509Credentials(c -> c.add(signing))
                    .decryptionX509Credentials(c -> c.add(decryption));

            var registration = registrationBuilder
                    .assertingPartyMetadata(this::assertingPartyMetadata)
                    .build();
            relyingPartyRegistrationRepository = new InMemoryRelyingPartyRegistrationRepository(registration);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * Reads the metadata of the identity provider.
     *
     * <p>A location on the web is read with a time limit. Without one, a server that accepts the connection and
     * never answers would hold the start of the application for ever. It happens when the metadata is served by
     * the application that is starting.
     *
     * @throws IOException when the server does not answer in time or the metadata cannot be read
     */
    static RelyingPartyRegistration.Builder fromMetadata(String location,
                                                         Duration connectTimeout,
                                                         Duration readTimeout) throws IOException {
        if (!location.startsWith("http://") && !location.startsWith("https://")) {
            return RelyingPartyRegistrations.fromMetadataLocation(location);
        }
        var connection = URI.create(location).toURL().openConnection();
        connection.setConnectTimeout((int) connectTimeout.toMillis());
        connection.setReadTimeout((int) readTimeout.toMillis());
        try (var metadata = connection.getInputStream()) {
            return RelyingPartyRegistrations.fromMetadata(metadata);
        } catch (SocketTimeoutException e) {
            throw new IOException("The SAML metadata at '%s' did not answer in time.".formatted(location), e);
        }
    }

    private void assertingPartyMetadata(AssertingPartyMetadata.Builder<?> party) {
        // Override certificate from the Metadata XML to prevent MITM attack.
        var serverCertificate = propertyResolver.getProperty("security.saml.server-certificate");
        if (serverCertificate != null && !serverCertificate.isBlank()) {
            try {
                X509Certificate idpCert = X509Support.decodeCertificate(serverCertificate);
                Saml2X509Credential verification = Saml2X509Credential.verification(idpCert);
                party.verificationX509Credentials(c -> {
                    c.clear();
                    c.add(verification);
                });
            } catch (CertificateException e) {
                throw new IllegalArgumentException("Failed to decode server certificate", e);
            }
        }
    }

    /**
     * Returns the relying party registration identified by the provided.
     * Initializes RelyingPartyRegistration if it has not been created yet.
     *
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
     *
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
