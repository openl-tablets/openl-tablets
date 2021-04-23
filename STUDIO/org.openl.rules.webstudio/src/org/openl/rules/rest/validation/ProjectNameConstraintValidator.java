package org.openl.rules.rest.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;

public class ProjectNameConstraintValidator implements ConstraintValidator<ProjectNameConstraint, String> {

    @Override
    public void initialize(ProjectNameConstraint constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isBlank(value)) {
            return true;
        }
        if (!NameChecker.checkName(value)) {
            context
                .buildConstraintViolationWithTemplate(
                    "Specified name is not a valid project name." + " " + NameChecker.BAD_NAME_MSG)
                .addConstraintViolation();
            return false;
        }
        if (NameChecker.isReservedName(value)) {
            context.buildConstraintViolationWithTemplate("Specified project name is a reserved word.")
                .addConstraintViolation();
            return false;
        }
        return true;
    }
}
