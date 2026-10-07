package org.openl.studio.projects.model.tables;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Requires the first cell of a raw table to contain a table header.
 * <p>
 * Compiled tables must start with a keyword OpenL recognizes, for example {@code Rules}, {@code Datatype} or
 * {@code Spreadsheet}. A raw table whose kind is {@code Other} is a deliberate free-form table and only requires a
 * non-blank header. The constraint is class-level because the header and kind belong to the complete raw table.
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
