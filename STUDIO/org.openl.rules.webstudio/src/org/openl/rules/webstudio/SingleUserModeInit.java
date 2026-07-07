package org.openl.rules.webstudio;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.security.NOPUserSettings;

/**
 * Creates a user for single user mode.
 *
 * <p>The user is created with the initial data from the {@code security.single.*} configuration and then
 * is synchronized in the same way as for the external authentication modes. A property defined by
 * a non-editable source, for example a Java system property or an environment variable, behaves as
 * an external field: it is applied on every startup and cannot be edited. Other fields stay editable
 * through the UI.
 */
@Component("singleUserModeInit")
@ConditionalOnProperty(name = "user.mode", havingValue = "single")
@RequiredArgsConstructor
public class SingleUserModeInit {

    private final UserManagementService userManagementService;

    @PostConstruct
    public void init() {
        var username = Props.text(NOPUserSettings.SINGLE_USERNAME);
        if (!userManagementService.existsByName(username)) {
            userManagementService.addUser(username,
                    Props.text(NOPUserSettings.SINGLE_FIRST_NAME),
                    Props.text(NOPUserSettings.SINGLE_LAST_NAME),
                    null,
                    Props.text(NOPUserSettings.SINGLE_EMAIL),
                    Props.text(NOPUserSettings.SINGLE_DISPLAY_NAME));
        }
        userManagementService.syncUserData(username,
                pinnedValue(NOPUserSettings.SINGLE_FIRST_NAME),
                pinnedValue(NOPUserSettings.SINGLE_LAST_NAME),
                pinnedValue(NOPUserSettings.SINGLE_EMAIL),
                pinnedValue(NOPUserSettings.SINGLE_DISPLAY_NAME));
    }

    /** Returns the property value when it is defined by a non-editable source, otherwise {@code null}. */
    private static String pinnedValue(String propertyName) {
        return Props.isDisabled(propertyName) ? Props.text(propertyName) : null;
    }
}
