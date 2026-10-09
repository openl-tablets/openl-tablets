package org.openl.rules.rest.validation;

import jakarta.annotation.Resource;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

import org.openl.rules.rest.model.InternalPasswordModel;
import org.openl.util.StringUtils;

/**
 * Checks the password of a new user.
 *
 * <p>When OpenL Studio stores the user credentials, the password is required and is at most 25 characters long. A
 * request without the password object is refused as one with a blank password.
 *
 * <p>When an external identity provider manages the credentials, the password is not used, so any value passes,
 * a missing one included.
 */
public class InternalPasswordConstraintValidator implements ConstraintValidator<InternalPasswordConstraint, InternalPasswordModel> {

    @Resource(name = "canCreateInternalUsers")
    protected boolean canCreateInternalUsers;

    @Override
    public void initialize(InternalPasswordConstraint constraintAnnotation) {
        // The constraint has no attributes to read.
    }

    @Override
    public boolean isValid(@Nullable InternalPasswordModel value, ConstraintValidatorContext context) {
        if (!canCreateInternalUsers) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        var password = value == null ? null : value.getPassword();
        if (StringUtils.isBlank(password)) {
            context.buildConstraintViolationWithTemplate("{jakarta.validation.constraints.NotBlank.message}")
                    .addConstraintViolation();
            return false;
        }
        if (password.length() > 25) {
            context.unwrap(HibernateConstraintValidatorContext.class)
                    .addMessageParameter("max", 25)
                    .buildConstraintViolationWithTemplate("{openl.constraints.size.max.message}")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

}
