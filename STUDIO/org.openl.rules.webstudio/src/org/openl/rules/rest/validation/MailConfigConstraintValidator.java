package org.openl.rules.rest.validation;

import javax.mail.Transport;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.rules.rest.model.MailConfigModel;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MailConfigConstraintValidator implements ConstraintValidator<MailConfigConstraint, MailConfigModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailConfigConstraintValidator.class);

    @Autowired
    private MailSender mailSender;

    @Override
    public void initialize(MailConfigConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(MailConfigModel value, ConstraintValidatorContext context) {
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
            context.buildConstraintViolationWithTemplate("{openl.constraints.mail.config.wrong.message}" + " " + e.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
