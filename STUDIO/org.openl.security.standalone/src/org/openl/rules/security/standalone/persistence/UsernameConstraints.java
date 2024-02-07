package org.openl.rules.security.standalone.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;

/**
 * Group of validation constraints and patterns for username to make it reusable in entity and model
 *
 * @author Vladyslav Pikus
 */
@Target({ElementType.METHOD,
        ElementType.FIELD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern.List({@Pattern(regexp = "[^.].*[^.]|[^.]", message = "{openl.constraints.username.2.message}"),
        @Pattern(regexp = "((?:.|\u2028|\u2029|\r|\n)(?<![.]{2}))+", message = "{openl.constraints.username.1.message}"),
        @Pattern(regexp = "([^/\\\\:*?\"<>|{}~^%;\u2028\u2029\\s])*", message = "{openl.constraints.username.3.message}")})
@Constraint(validatedBy = {})
public @interface UsernameConstraints {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
