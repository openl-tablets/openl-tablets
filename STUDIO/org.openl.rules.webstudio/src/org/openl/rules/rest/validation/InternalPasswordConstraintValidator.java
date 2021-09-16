package org.openl.rules.rest.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.rules.rest.model.InternalPasswordModel;
import org.openl.util.StringUtils;

public class InternalPasswordConstraintValidator implements ConstraintValidator<InternalPasswordConstraint, InternalPasswordModel> {

    @Override
    public void initialize(InternalPasswordConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(InternalPasswordModel value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isNotBlank(value.getPassword())) {
            if (value.getPassword().length() > 25) {
                context.buildConstraintViolationWithTemplate("{openl.constraints.user.field.max-length.message}")
                    .addConstraintViolation();
                return false;
            }
        } else if (value.isInternalUser()) {
            context.buildConstraintViolationWithTemplate("{openl.constraints.user.field.empty.message}")
                .addConstraintViolation();
            return false;
        }
        return true;
    }

}
