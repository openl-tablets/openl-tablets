package org.openl.rules.ui.validation;

import org.openl.rules.utils.TableNameChecker;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
