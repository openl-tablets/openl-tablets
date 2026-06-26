package org.openl.studio.projects.model.tables;

import java.util.Date;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Accepts only the cell values a table can hold: {@code null}, a string, a number, a boolean or a date.
 * <p>
 * Over JSON a date arrives as a string or a number (Jackson never binds a value to {@link Date} here), so the date
 * branch is there for fidelity to the grid's writers rather than reached by a request.
 *
 * @author Vladyslav Pikus
 */
public class CellValueConstraintValidator implements ConstraintValidator<CellValueConstraint, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Date;
    }
}
