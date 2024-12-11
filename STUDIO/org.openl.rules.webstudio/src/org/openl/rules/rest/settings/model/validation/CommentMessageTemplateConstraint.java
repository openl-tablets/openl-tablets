package org.openl.rules.rest.settings.model.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = CommentMessageTemplateConstraintValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommentMessageTemplateConstraint {

    String message() default "Invalid comment message template";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
