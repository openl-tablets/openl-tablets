package org.openl.rules.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import lombok.RequiredArgsConstructor;

import org.openl.rules.webstudio.service.UserManagementService;

@RequiredArgsConstructor
public class UsernameExistsConstraintValidator implements ConstraintValidator<UsernameExistsConstraint, String> {

    private final UserManagementService userManagementService;

    @Override
    public void initialize(UsernameExistsConstraint constraintAnnotation) {
        // The constraint has no attributes to read.
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !userManagementService.existsByName(value);
    }
}
