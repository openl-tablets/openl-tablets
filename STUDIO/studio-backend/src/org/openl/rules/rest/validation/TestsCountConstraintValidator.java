package org.openl.rules.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.jspecify.annotations.Nullable;

/**
 * Accepts {@code -1}, which lists all test results or failures, and any positive number.
 *
 * @author Yury Molchan
 */
public class TestsCountConstraintValidator implements ConstraintValidator<TestsCountConstraint, Integer> {

    private static final int ALL = -1;

    @Override
    public boolean isValid(@Nullable Integer value, ConstraintValidatorContext context) {
        return value == null || value == ALL || value > 0;
    }
}
