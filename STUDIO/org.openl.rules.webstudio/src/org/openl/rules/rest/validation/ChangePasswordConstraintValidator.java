package org.openl.rules.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.studio.security.CurrentUserInfo;
import org.openl.util.StringUtils;

@RequiredArgsConstructor
public class ChangePasswordConstraintValidator implements ConstraintValidator<ChangePasswordConstraint, ChangePasswordModel> {

    private final UserManagementService userManagementService;

    private final PasswordEncoder passwordEncoder;

    private final CurrentUserInfo currentUserInfo;

    @Override
    public void initialize(ChangePasswordConstraint constraintAnnotation) {
        // The constraint has no attributes to read.
    }

    @Override
    public boolean isValid(ChangePasswordModel value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isNotEmpty(value.getNewPassword()) || StringUtils
                .isNotEmpty(value.getCurrentPassword()) || StringUtils.isNotEmpty(value.getConfirmPassword())) {
            var userPasswordHash = userManagementService.getUser(currentUserInfo.getUserName()).getPassword();

            if (StringUtils.isEmpty(value.getCurrentPassword())) {
                context.buildConstraintViolationWithTemplate("{openl.constraints.password.empty.message}")
                        .addConstraintViolation();
                return false;
            }

            if (!value.getNewPassword().equals(value.getConfirmPassword())) {
                context.buildConstraintViolationWithTemplate("{openl.constraints.password.not-match.message}")
                        .addConstraintViolation();
                return false;
            }

            if (!passwordEncoder.matches(value.getCurrentPassword(), userPasswordHash)) {
                context.buildConstraintViolationWithTemplate("{openl.constraints.password.wrong-current.message}")
                        .addConstraintViolation();
                return false;
            }

        }
        return true;
    }

}
