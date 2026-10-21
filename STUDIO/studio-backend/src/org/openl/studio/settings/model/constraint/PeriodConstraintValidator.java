package org.openl.studio.settings.model.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.jspecify.annotations.Nullable;

import org.openl.studio.common.utils.Periods;

/**
 * Accepts a period longer than zero, or no period at all.
 */
public class PeriodConstraintValidator implements ConstraintValidator<PeriodConstraint, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        try {
            Periods.parse(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
