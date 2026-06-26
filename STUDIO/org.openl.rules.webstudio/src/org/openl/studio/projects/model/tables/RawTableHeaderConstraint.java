package org.openl.studio.projects.model.tables;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Requires the first cell of a raw table to start with a keyword OpenL recognizes as a table type.
 * <p>
 * OpenL identifies a table by the first token of its top-left cell. Without a known header (for example
 * {@code Rules}, {@code Datatype} or {@code Spreadsheet}) OpenL never compiles the table, so it would be written to
 * the project yet stay invisible. The constraint is class-level because the header lives at the top-left of the
 * source matrix.
 *
 * @author Vladyslav Pikus
 */
@Documented
@Constraint(validatedBy = RawTableHeaderConstraintValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RawTableHeaderConstraint {

    String message() default "{openl.error.400.table.header.unrecognized.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
