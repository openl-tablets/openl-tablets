package org.openl.rules.rest.settings.model.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = DBConnectionConstraintValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DBConnectionConstraint {

    String message() default "Incorrect database URL, login or password.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
