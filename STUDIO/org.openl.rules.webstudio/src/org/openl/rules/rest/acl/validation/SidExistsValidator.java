package org.openl.rules.rest.acl.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.security.acls.model.Sid;

import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;

public class SidExistsValidator extends ASidExistsValidator implements ConstraintValidator<SidExistsConstraint, Sid> {

    public SidExistsValidator(UserManagementService userService, GroupManagementService groupService) {
        super(userService, groupService);
    }

    @Override
    public boolean isValid(Sid sid, ConstraintValidatorContext ctx) {
        return super.isValid(sid, ctx);
    }
}
