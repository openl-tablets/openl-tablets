package org.openl.rules.rest.settings.model.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = CertificateConstraintValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CertificateConstraint {

    String message() default "Entered SAML server certificate is not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
