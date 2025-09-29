package org.openl.rules.rest.validation;

import java.io.IOException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.util.SystemReader;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;

public class PathConstraintValidator implements ConstraintValidator<PathConstraint, String> {

    private boolean allowTrailingSlash;
    private boolean allowLeadingSlash;

    @Override
    public void initialize(PathConstraint constraintAnnotation) {
        allowTrailingSlash = constraintAnnotation.allowTrailingSlash();
        allowLeadingSlash = constraintAnnotation.allowLeadingSlash();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isEmpty(value)) {
            return true;
        }
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
