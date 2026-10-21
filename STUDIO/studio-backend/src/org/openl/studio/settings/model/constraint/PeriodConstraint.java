package org.openl.studio.settings.model.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * A setting that holds a period longer than zero, as {@link org.openl.studio.common.utils.Periods} reads it: a number
 * with its unit, such as {@code 90d} or {@code 1y}, or a period in ISO 8601. A number without its unit is refused.
 *
 * <p>An empty value passes: it stands for no period. A setting that needs one adds {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = PeriodConstraintValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PeriodConstraint {

    String message() default "{openl.constraints.period.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
