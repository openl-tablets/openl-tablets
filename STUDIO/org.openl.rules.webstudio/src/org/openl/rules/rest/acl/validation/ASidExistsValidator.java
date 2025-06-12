package org.openl.rules.rest.acl.validation;

import javax.validation.ConstraintValidatorContext;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;

public abstract class ASidExistsValidator {

    private final UserManagementService userService;
    private final GroupManagementService groupService;

    protected ASidExistsValidator(UserManagementService userService, GroupManagementService groupService) {
        this.userService = userService;
        this.groupService = groupService;
    }

    protected boolean isValid(Sid sid, ConstraintValidatorContext ctx) {
        return switch (sid) {
            case null -> true;
            case PrincipalSid user -> validateUser(user, ctx);
            case GrantedAuthoritySid group -> validateGroup(group, ctx);
            default -> throw new IllegalStateException("Unsupported Sid type: " + sid.getClass());
        };
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
