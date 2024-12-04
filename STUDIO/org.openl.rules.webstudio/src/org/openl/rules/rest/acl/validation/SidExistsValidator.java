package org.openl.rules.rest.acl.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;

public class SidExistsValidator implements ConstraintValidator<SidExistsConstraint, Sid> {

    private final UserManagementService userService;
    private final GroupManagementService groupService;

    public SidExistsValidator(UserManagementService userService, GroupManagementService groupService) {
        this.userService = userService;
        this.groupService = groupService;
    }

    @Override
    public void initialize(SidExistsConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(Sid sid, ConstraintValidatorContext ctx) {
        if (sid == null) {
            return true;
        }
        if (sid instanceof PrincipalSid) {
            return validateUser((PrincipalSid) sid, ctx);
        } else if (sid instanceof GrantedAuthoritySid) {
            return validateGroup((GrantedAuthoritySid) sid, ctx);
        }
        throw new IllegalArgumentException("Unsupported Sid type: " + sid.getClass());
    }

    private boolean validateUser(PrincipalSid sid, ConstraintValidatorContext ctx) {
        if (!userService.existsByName(sid.getPrincipal())) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("{openl.constraints.principal-sid.not.exists.message}")
                .addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean validateGroup(GrantedAuthoritySid sid, ConstraintValidatorContext ctx) {
        if (!groupService.existsByName(sid.getGrantedAuthority())) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("{openl.constraints.granted-authority-sid.not.exists.message}")
                .addConstraintViolation();
            return false;
        }
        return true;
    }
}
