package org.openl.rules.rest.validation;

import java.io.IOException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.util.SystemReader;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;

public class PathConstraintValidator implements ConstraintValidator<PathConstraint, String> {

    @Override
    public void initialize(PathConstraint constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        if (value.startsWith("/")) {
            context.buildConstraintViolationWithTemplate("Path in repository cannot start with '/'")
                .addConstraintViolation();
            return false;
        }
        try {
            // Cross-platform path check
            NameChecker.validatePath(value);
        } catch (IOException e) {
            context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
            return false;
        }
        try {
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            // Git specifics and non-cross-platform check if we missed something before
            SystemReader.getInstance().checkPath(value);
        } catch (CorruptObjectException e) {
            context.buildConstraintViolationWithTemplate(StringUtils.capitalize(e.getMessage()))
                .addConstraintViolation();
        }
        return true;
    }
}
