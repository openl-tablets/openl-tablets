package org.openl.rules.webstudio.web.install;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.Period;
import java.util.Base64;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Generate KeyPair and X509Certificate.
 *
 * @author Eugene Biruk
 * @author Yury Molchan
 */
@Slf4j
public class KeyPairCertUtils {

    private KeyPairCertUtils() {
        // Utility class
    }

    private static Pair<String, String> generate()
            throws GeneralSecurityException, IOException, OperatorCreationException {

        // Generate a RSA private key with 4096 bit size
        var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(4096);
        var keyPair = kpg.genKeyPair();
        var publicKey = keyPair.getPublic();
        var privateKey = keyPair.getPrivate();
        var privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        // Define period of certificate validity during 10 years in days.
        // Period in 10 years is enough for testing/local usage without re-installation.
        // Minus 2 days is for the case when time on an application server is wrongly synced with IdP server time.
        var now = Instant.now();
        var notBefore = Date.from(now.minus(Period.ofDays(2)));
        var notAfter = Date.from(now.plus(Period.ofDays(10 * 365)));

        // Self-signed, so an issuer and a subject are the same
        var issuer = new X500Name("CN=webstudio");

        var builder = new JcaX509v3CertificateBuilder(issuer, BigInteger.valueOf(now.toEpochMilli()), notBefore, notAfter, issuer, publicKey);

        var x509ExtensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.subjectKeyIdentifier, false, x509ExtensionUtils.createSubjectKeyIdentifier(publicKey));
        builder.addExtension(Extension.authorityKeyIdentifier, false, x509ExtensionUtils.createAuthorityKeyIdentifier(publicKey));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

        var contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
        var x509CertificateHolder = builder.build(contentSigner);

        var certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);
        var certBase64 = Base64.getEncoder().encodeToString(certificate.getEncoded());

        return Pair.of(privateKeyBase64, certBase64);
    }

    /**
     * Generates a private key and its certificate in base64.
     *
     * @return Left-key - private key, Right-value - certificate.
     */
    public static Pair<String, String> generateCertificate() {
        try {
            return generate();
        } catch (Exception e) {
            log.error("Cannot generate X.509 certificate for the application", e);
            return null;
        }
    }

    public static @Nullable Pair<String, String> renewUnreadable(@Nullable String privateKey,
                                                                @Nullable String certificate) {
        if (isReadable(privateKey, certificate)) {
            return null;
        }
        if (privateKey != null) {
            log.warn("The SAML key pair of the service provider is incomplete or cannot be read, for instance after"
                    + " secret.key has changed or has been removed. A new key pair is generated: give the identity"
                    + " provider the new metadata of the service provider.");
        }
        return generateCertificate();
    }

    private static boolean isReadable(@Nullable String privateKey, @Nullable String certificate) {
        if (privateKey == null || privateKey.isBlank() || certificate == null || certificate.isBlank()) {
            return false;
        }
        try {
            KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(privateKey)));
            CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(certificate)));
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
