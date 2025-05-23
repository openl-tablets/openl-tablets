package org.openl.rules.rest.settings.model.validation;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
        } catch (IllegalArgumentException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid Base64 encoding")
                    .addConstraintViolation();
        } catch (CertificateExpiredException |
                 CertificateNotYetValidException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Certificate is not valid: " + e.getMessage())
                    .addConstraintViolation();
        } catch (Exception e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid certificate format")
                    .addConstraintViolation();
        }
        return false;
    }
}
