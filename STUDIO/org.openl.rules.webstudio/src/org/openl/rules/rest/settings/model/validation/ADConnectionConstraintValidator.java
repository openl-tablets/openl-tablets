package org.openl.rules.rest.settings.model.validation;

import javax.naming.directory.InvalidSearchFilterException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import org.openl.rules.webstudio.web.admin.security.ADAuthenticationSettings;

public class ADConnectionConstraintValidator implements ConstraintValidator<ADConnectionConstraint, ADAuthenticationSettings> {

    @Override
    public boolean isValid(ADAuthenticationSettings settings, ConstraintValidatorContext ctx) {
        var creds = settings.getCredentials();
        if (creds == null) {
            // Skip validation if credentials are missing
            return true;
        }
        try {
            var ldapAuthenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(
                    settings.getDomain(),
                    settings.getServerUrl());
            ldapAuthenticationProvider.setSearchFilter(settings.getSearchFilter());

            var authenticationToken = new UsernamePasswordAuthenticationToken(
                    creds.getUsername(),
                    creds.getPassword());
            ldapAuthenticationProvider.authenticate(authenticationToken);
            return true;
        } catch (AuthenticationException e) {
            ctx.disableDefaultConstraintViolation();
            if (e.getCause() instanceof InvalidSearchFilterException cause) {
                String message = "Invalid search filter: " + cause.getMessage();
                ctx.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode("searchFilter")
                        .addConstraintViolation();
            } else {
                ctx.buildConstraintViolationWithTemplate(e.getMessage())
                        .addConstraintViolation();
            }
        } catch (RuntimeException e) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(extractRootMessage(e))
                    .addConstraintViolation();
        }
        return false;
    }

    private String extractRootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : t.toString();
    }
}
