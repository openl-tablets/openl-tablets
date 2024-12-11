package org.openl.rules.rest.settings.model.validation;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.openl.util.StringUtils;

public class CertificateConstraintValidator implements ConstraintValidator<CertificateConstraint, String> {

    @Override
    public boolean isValid(String publicServerCert, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(publicServerCert)) {
            return true; // Optional field; skip if empty
        }

        try {
            var cf = CertificateFactory.getInstance("X.509");
            byte[] decoded = Base64.getMimeDecoder().decode(publicServerCert);
            var cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
            cert.checkValidity(); // check if certificate is currently valid
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
