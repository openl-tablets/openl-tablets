package org.openl.rules.rest.validation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.util.SystemReader;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;

public class PathConstraintValidator implements ConstraintValidator<PathConstraint, String> {

    private boolean allowTrailingSlash;
    private boolean allowLeadingSlash;
    private Set<String> allowedSchemes;

    @Override
    public void initialize(PathConstraint constraintAnnotation) {
        allowTrailingSlash = constraintAnnotation.allowTrailingSlash();
        allowLeadingSlash = constraintAnnotation.allowLeadingSlash();
        allowedSchemes = Arrays.stream(constraintAnnotation.allowedSchemes())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        if (! allowedSchemes.isEmpty()) {
            try {
                URI uri = new URI(value);
                //If scheme is null, we validate it as a local path
                if (uri.getScheme() != null) {
                    if (allowedSchemes.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
                        return true;
                    } else {
                        String listOfAllowedSchemes = String.join(", ", allowedSchemes);

                        context.unwrap(HibernateConstraintValidatorContext.class)
                                .addMessageParameter("scheme", uri.getScheme())
                                .addMessageParameter("allowedSchemes", listOfAllowedSchemes) 
                                .buildConstraintViolationWithTemplate("{openl.constraints.path.incorrect.scheme.message}")
                                .addConstraintViolation();

                        return false;
                    }
                }
            } catch (URISyntaxException e) {
                //do nothing, check it as a path
            }
        }
        //Checking path
        boolean basicCheck = true;
        if (!allowLeadingSlash && value.startsWith("/")) {
            context.buildConstraintViolationWithTemplate("{openl.constraints.path.1.message}").addConstraintViolation();
            basicCheck = false;
        }
        if (!allowTrailingSlash && value.endsWith("/")) {
            context.buildConstraintViolationWithTemplate("{openl.constraints.path.2.message}").addConstraintViolation();
            basicCheck = false;
        }
        if (!basicCheck) {
            return false;
        }
        try {
            // Cross-platform path check
            NameChecker.validatePath(value);
        } catch (IOException | IllegalArgumentException e) {
            context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
            return false;
        }
        try {
            // Git specifics and non-cross-platform check if we missed something before
            SystemReader.getInstance().checkPath(value);
        } catch (CorruptObjectException e) {
            context.buildConstraintViolationWithTemplate(StringUtils.capitalize(e.getMessage()))
                    .addConstraintViolation();
        }
        return true;
    }
}
