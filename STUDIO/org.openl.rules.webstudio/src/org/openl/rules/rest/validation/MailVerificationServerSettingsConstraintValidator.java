package org.openl.rules.rest.validation;

import jakarta.mail.Transport;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.web.admin.MailVerificationServerSettings;
import org.openl.util.StringUtils;

public class MailVerificationServerSettingsConstraintValidator implements ConstraintValidator<MailConfigConstraint, MailVerificationServerSettings> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailVerificationServerSettingsConstraintValidator.class);

    @Autowired
    private MailSender mailSender;

    @Override
    public void initialize(MailConfigConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(MailVerificationServerSettings value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        // If all properties are empty - is OK, no configuration is defined
        if (StringUtils.isEmpty(value.getUrl()) && StringUtils.isEmpty(value.getUsername()) && StringUtils.isEmpty(value.getPassword())) {
            return true;
        }

        // If some is missed - the error
        if (StringUtils.isBlank(value.getUrl()) || StringUtils.isBlank(value.getUsername()) || StringUtils.isBlank(value.getPassword())) {
            context.buildConstraintViolationWithTemplate("{openl.constraints.mail.config.not-empty.message}")
                    .addConstraintViolation();
            return false;
        }

        // Finally, try to connect to the mail server to validate settings
        try (Transport transport = mailSender.getTransport(value.getUrl(), value.getUsername(), value.getPassword())) {
            return transport.isConnected();
        } catch (Exception e) {
            LOGGER.warn("Error on changing email server configuration: ", e);
            context.unwrap(HibernateConstraintValidatorContext.class)
                    .addMessageParameter("error", e.getMessage())
                    .buildConstraintViolationWithTemplate("{openl.constraints.mail.config.wrong.message}")
                    .addConstraintViolation();
            return false;
        }
    }
}
