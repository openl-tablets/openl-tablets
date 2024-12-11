package org.openl.rules.rest.acl.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = {
        SidExistsValidator.class,
        AclSubjectExistsValidator.class
})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SidExistsConstraint {

    String message() default "Sid does not exist";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
