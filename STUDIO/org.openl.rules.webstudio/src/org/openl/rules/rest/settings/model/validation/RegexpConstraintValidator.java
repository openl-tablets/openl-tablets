package org.openl.rules.rest.settings.model.validation;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jakarta.validation.ConstraintValidator;

public class RegexpConstraintValidator implements ConstraintValidator<RegexpConstraint, String> {

    @Override
    public boolean isValid(String value, jakarta.validation.ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            Pattern.compile(value);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
