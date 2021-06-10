package org.openl.rules.rest.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ProjectNameConstraintValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProjectNameConstraint {

    String message() default "{openl.constraints.ProjectNameConstraint.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
