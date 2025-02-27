package org.openl.rules.rest.settings.model.validation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Validates directory for write access. If specified folder is not writable the validation error will appears
 */
@Documented
@Constraint(validatedBy = WriteDirectoryConstraintValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WriteDirectoryConstraint {

    String message() default "{}";

    String directoryType();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
