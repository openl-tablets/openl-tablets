package org.openl.studio.projects.model.tables;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Requires a name OpenL can compile a table under.
 *
 * <p>A table name is an identifier: it opens with a letter, {@code _} or {@code $} and carries letters, digits,
 * {@code _} and {@code $} after it. A name of any other shape reaches the workbook and stops the module from
 * compiling, so it is refused before it is written.
 *
 * @author Vladyslav Pikus
 */
@Documented
@Constraint(validatedBy = TableNameConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableNameConstraint {

    String message() default "{openl.error.400.table.name.invalid.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
