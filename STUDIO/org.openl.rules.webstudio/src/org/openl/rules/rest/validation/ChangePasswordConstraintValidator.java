package org.openl.rules.rest.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ChangePasswordConstraintValidator implements ConstraintValidator<ChangePasswordConstraint, ChangePasswordModel> {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CurrentUserInfo currentUserInfo;

    @Override
    public void initialize(ChangePasswordConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(ChangePasswordModel value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (StringUtils.isNotEmpty(value.getNewPassword()) || StringUtils
            .isNotEmpty(value.getCurrentPassword()) || StringUtils.isNotEmpty(value.getConfirmPassword())) {
            String userPasswordHash = userManagementService.getApplicationUser(currentUserInfo.getUserName())
                .getPassword();

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
