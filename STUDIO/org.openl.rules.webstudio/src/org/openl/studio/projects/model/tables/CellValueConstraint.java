package org.openl.studio.projects.model.tables;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restricts a cell value to a type that can be written into a table cell: a string, a number, a boolean, a date or a
 * one-dimensional array of these values. Dates are internal grid values. JSON requests represent date values using
 * string or number scalars.
 * <p>
 * A {@code null} value is allowed (it clears the cell). Arrays must not be empty or consist of one null because the
 * workbook format cannot distinguish those values from an empty cell. String elements must be non-empty and carry no
 * surrounding whitespace because OpenL trims array elements. Objects and nested arrays are rejected.
 *
 * @author Vladyslav Pikus
 */
@Documented
@Constraint(validatedBy = CellValueConstraintValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CellValueConstraint {

    String message() default "{openl.error.400.table.action.cell.value.type.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
