package org.openl.studio.security;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Updates user details in the DB.
 */
@RequiredArgsConstructor
@Slf4j
@Service("syncUserData")
public class SyncUserData implements Consumer<SimpleUser> {
    private final UserManagementService userManagementService;
    private final AdminUsers adminUsersInitializer;
    private final ExternalGroupService externalGroupService;

    @Override
    public void accept(SimpleUser user) {

        var username = user.getUsername();
        var firstName = user.getFirstName();
        var lastName = user.getLastName();
        var email = user.getEmail();
        var displayName = user.getDisplayName();

        // Update User details
        userManagementService.syncUserData(username,
                fit(username, "first name", firstName, User.NAME_LENGTH),
                fit(username, "last name", lastName, User.NAME_LENGTH),
                fitEmail(username, email),
                fit(username, "display name", displayName, User.DISPLAY_NAME_LENGTH));

        // Initialize admin privileges
        adminUsersInitializer.initIfSuperuser(username);

        // Store all external authorities
        externalGroupService.mergeAllForUser(username, user.getAuthorities());

    }

    /**
     * Cuts a value the identity provider gives down to what the users table keeps, so that a long name does not
     * fail the sign-in.
     */
    private static @Nullable String fit(String username, String field, @Nullable String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        // A cut between the two halves of a character outside the basic plane would leave a broken text.
        var end = Character.isHighSurrogate(value.charAt(maxLength - 1)) ? maxLength - 1 : maxLength;
        log.warn("The {} of the user '{}' has {} characters and is cut to {}.", field, username, value.length(), end);
        return value.substring(0, end);
    }

    /**
     * Leaves out an e-mail the users table cannot keep. A cut address could name the mailbox of somebody else, so
     * it is not stored at all.
     */
    private static @Nullable String fitEmail(String username, @Nullable String email) {
        if (email == null || email.length() <= User.EMAIL_LENGTH) {
            return email;
        }
        log.warn("The e-mail of the user '{}' has {} characters, which is more than {}, and is not stored.", username,
                email.length(), User.EMAIL_LENGTH);
        return null;
    }

}
