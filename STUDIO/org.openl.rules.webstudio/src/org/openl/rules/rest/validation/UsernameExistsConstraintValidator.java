package org.openl.rules.rest.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.rules.webstudio.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;

public class UsernameExistsConstraintValidator implements ConstraintValidator<UsernameExistsConstraint, String> {

    @Autowired
    private UserManagementService userManagementService;

    @Override
    public void initialize(UsernameExistsConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return userManagementService.getApplicationUser(value) == null;
    }
}
