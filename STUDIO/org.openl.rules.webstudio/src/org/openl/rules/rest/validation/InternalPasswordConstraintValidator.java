package org.openl.rules.rest.validation;

import javax.annotation.Resource;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.openl.rules.rest.model.InternalPasswordModel;
import org.openl.util.StringUtils;

public class InternalPasswordConstraintValidator implements ConstraintValidator<InternalPasswordConstraint, InternalPasswordModel> {

    @Resource(name = "canCreateInternalUsers")
    protected boolean canCreateInternalUsers;

    @Override
    public void initialize(InternalPasswordConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(InternalPasswordModel value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isNotBlank(value.getPassword())) {
            if (value.getPassword().length() > 25) {
                context.unwrap(HibernateConstraintValidatorContext.class)
                    .addMessageParameter("max", 25)
                    .buildConstraintViolationWithTemplate("{openl.constraints.size.max.message}")
                    .addConstraintViolation();
                return false;
            }
        } else if (canCreateInternalUsers) {
            context.buildConstraintViolationWithTemplate("{org.hibernate.validator.constraints.NotBlank.message}")
                .addConstraintViolation();
            return false;
        }
        return true;
    }

}
