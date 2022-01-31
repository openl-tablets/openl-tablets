package org.openl.rules.rest.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = MailConfigConstraintValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MailConfigConstraint {

    String message() default "{openl.constraints.mail.config.wrong.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
