package org.openl.rules.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import org.openl.rules.webstudio.service.UserManagementService;

public class UsernameExistsConstraintValidator implements ConstraintValidator<UsernameExistsConstraint, String> {

    @Autowired
    private UserManagementService userManagementService;

    @Override
    public void initialize(UsernameExistsConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !userManagementService.existsByName(value);
    }
}
