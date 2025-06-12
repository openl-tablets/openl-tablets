package org.openl.rules.rest.acl.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.rules.rest.acl.model.AclSubject;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;

public class AclSubjectExistsValidator extends ASidExistsValidator implements ConstraintValidator<SidExistsConstraint, AclSubject> {

    public AclSubjectExistsValidator(UserManagementService userService, GroupManagementService groupService) {
        super(userService, groupService);
    }

    @Override
    public boolean isValid(AclSubject subject, ConstraintValidatorContext ctx) {
        if (subject == null) {
            return true;
        }
        return super.isValid(subject.toSid(), ctx);
    }
}
