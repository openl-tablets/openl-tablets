package org.openl.rules.ui.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.openl.util.TableNameChecker;

public class TableNameValidator implements ConstraintValidator<TableNameConstraint, String> {

    @Override
    public void initialize(TableNameConstraint constraintAnnotation) {
        // there is no default values, etc.
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return TableNameChecker.isValidJavaIdentifier(value);
    }
}
