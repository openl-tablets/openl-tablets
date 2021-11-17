package org.openl.security.saml;

import org.opensaml.security.x509.X509Support;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.DefaultSaml2AuthenticationRequestContextResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class OpenLSamlBuilder {

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    public OpenLSamlBuilder(PropertyResolver propertyResolver) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(propertyResolver.getProperty("security.saml.local_key")));
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(propertyResolver.getProperty("security.saml.local-certificate")));
        X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(in);

        X509Certificate certificate2 = X509Support.decodeCertificate(propertyResolver.getProperty("security.saml.server-certificate"));
        Saml2X509Credential verification = Saml2X509Credential.verification(certificate2);

        Saml2X509Credential signing = Saml2X509Credential.signing(privateKey, certificate);
        Saml2X509Credential decryption = Saml2X509Credential.decryption(privateKey, certificate);
        RelyingPartyRegistration registration = RelyingPartyRegistrations
            .fromMetadataLocation(propertyResolver.getProperty("security.saml.saml-server-metadata-url"))
            .registrationId("webstudio")
            .entityId(propertyResolver.getProperty("security.saml.entity-id"))
            .signingX509Credentials(c -> c.add(signing))
            .decryptionX509Credentials(c -> c.add(decryption))
            .assertingPartyDetails(party -> party
                .verificationX509Credentials(c -> c.add(verification))
            )
            .build();
        relyingPartyRegistrationRepository = new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    public InMemoryRelyingPartyRegistrationRepository relyingPartyRegistration() {
        return relyingPartyRegistrationRepository;
    }

    public Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver() {
        return new DefaultSaml2AuthenticationRequestContextResolver(relyingPartyRegistrationResolver());
    }

    public RelyingPartyRegistrationResolver relyingPartyRegistrationResolver() {
        return new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);
    }

    public Saml2AuthenticationRequestFactory authenticationRequestFactory() {
        return new OpenSamlAuthenticationRequestFactory();
    }

}
