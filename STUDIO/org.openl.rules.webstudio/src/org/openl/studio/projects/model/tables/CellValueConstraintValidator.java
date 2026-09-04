package org.openl.studio.projects.model.tables;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.openl.util.StringUtils;

/**
 * Accepts only the cell values a table can hold: {@code null}, a string, a number, a boolean, a date or a
 * one-dimensional array of these values.
 * <p>
 * Over JSON a date arrives as a string or a number (Jackson never binds a value to {@link Date} here), so the date
 * branch is there for fidelity to the grid's writers rather than reached by a request.
 *
 * @author Vladyslav Pikus
 */
public class CellValueConstraintValidator implements ConstraintValidator<CellValueConstraint, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (isScalar(value)) {
            return true;
        }
        if (value instanceof Collection<?> values) {
            return isValidArray(values.toArray());
        }
        return value instanceof Object[] values && isValidArray(values);
    }

    private static boolean isValidArray(Object[] values) {
        return values.length > 0
                && (values.length > 1 || values[0] != null)
                && Arrays.stream(values).allMatch(CellValueConstraintValidator::isArrayElement);
    }

    private static boolean isArrayElement(Object value) {
        return isScalar(value)
                && (!(value instanceof String text) || text.equals(StringUtils.trim(text)) && !text.isEmpty());
    }

    private static boolean isScalar(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Date;
    }
}
