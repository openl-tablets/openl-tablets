package org.openl.studio.settings.model.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = ADConnectionConstraintValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ADConnectionConstraint {

    String message() default "Incorrect Active Directory URL, login or password.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
