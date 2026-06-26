package org.openl.studio.projects.model.tables;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restricts a cell value to a type that can be written into a table cell: a string, a number or a boolean.
 * <p>
 * A {@code null} value is allowed (it clears the cell). Objects and arrays are rejected, since the grid would only
 * store their {@code toString()} form as a cell of text.
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
