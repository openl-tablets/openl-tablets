package org.openl.rules.rest.validation;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.openl.config.InMemoryProperties;
import org.openl.rules.rest.model.UserEditModel;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Edit user model validator
 *
 * @author Vladyslav Pikus
 */
@Component
public class UserEditModelValidator implements Validator {

    private static final Set<String> SUPPORT_EXTERNAL_GROUPS = Stream.of("ad", "saml", "cas")
        .collect(Collectors.toSet());

    private final CurrentUserInfo currentUserInfo;
    private final ExternalGroupService extGroupService;
    private final AdminUsers adminUsersInitializer;
    private final UserManagementService userMngmtService;
    private final InMemoryProperties properties;

    @Inject
    public UserEditModelValidator(CurrentUserInfo currentUserInfo,
            ExternalGroupService extGroupService,
            AdminUsers adminUsersInitializer,
            UserManagementService userMngmtService,
            InMemoryProperties properties) {
        this.currentUserInfo = currentUserInfo;
        this.extGroupService = extGroupService;
        this.adminUsersInitializer = adminUsersInitializer;
        this.userMngmtService = userMngmtService;
        this.properties = properties;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == UserEditModel.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        UserEditModel editModel = (UserEditModel) target;
        if (editModel.getGroups() != null && !editModel.getGroups().isEmpty()) {
            return; // OK
        }

        if (StringUtils.isNotBlank(properties.getProperty("security.default-group"))) {
            return; // OK
        }

        if (SUPPORT_EXTERNAL_GROUPS.contains(properties.getProperty("user.mode"))) {
            // Check external groups if exist
            if (extGroupService.countMatchedForUser(editModel.getUsername()) > 0) {
                return; // OK
            }
        }

        final boolean leaveAdminGroups = editModel.getUsername() != null && (adminUsersInitializer.isSuperuser(
            editModel.getUsername()) || Objects.equals(currentUserInfo.getUserName(), editModel.getUsername()));
        if (leaveAdminGroups) {
            // Check current admin groups if exist
            User user = userMngmtService.getUser(editModel.getUsername());
            if (!userMngmtService.getCurrentAdminGroups(user.getGroups()).isEmpty()) {
                return; // OK
            }
        }

        errors.rejectValue("groups", "user.groups.empty.message");
    }

}
