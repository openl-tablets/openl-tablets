package org.openl.rules.rest.validation;

import java.io.IOException;
import java.nio.file.Paths;

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
        boolean basicCheck = true;
        if (value.startsWith("/")) {
            context.buildConstraintViolationWithTemplate("{openl.constraints.path.1.message}").addConstraintViolation();
            basicCheck = false;
        }
        if (value.endsWith("/")) {
            context.buildConstraintViolationWithTemplate("{openl.constraints.path.2.message}").addConstraintViolation();
            basicCheck = false;
        }
        if (!basicCheck) {
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
            // Git specifics and non-cross-platform check if we missed something before
            SystemReader.getInstance().checkPath(value);
        } catch (CorruptObjectException e) {
            context.buildConstraintViolationWithTemplate(StringUtils.capitalize(e.getMessage()))
                .addConstraintViolation();
        }
        try {
            // OS specific check
            Paths.get(value);
        } catch (IllegalArgumentException e) {
            context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
            return false;
        }
        return true;
    }
}
