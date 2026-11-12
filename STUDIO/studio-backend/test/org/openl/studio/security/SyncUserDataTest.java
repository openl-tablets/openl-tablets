package org.openl.studio.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Checks what of a user is stored when the user signs in through an identity provider.
 *
 * @author Yury Molchan
 */
class SyncUserDataTest {

    private final UserManagementService users = mock(UserManagementService.class);
    private final SyncUserData sync = new SyncUserData(users, mock(AdminUsers.class), mock(ExternalGroupService.class));

    @Test
    void storesTheDetailsAsTheyAreGiven() {
        sync.accept(user("Jane", "Doe", "jane@example.com", "Jane Doe"));

        verify(users).syncUserData("jdoe", "Jane", "Doe", "jane@example.com", "Jane Doe");
    }

    /** A name longer than the users table keeps is cut, so that the sign-in does not fail on it. */
    @Test
    void cutsTheDetailsTheUsersTableCannotKeep() {
        sync.accept(user("f".repeat(100), "l".repeat(51), "jane@example.com", "d".repeat(65)));

        verify(users).syncUserData("jdoe", "f".repeat(50), "l".repeat(50), "jane@example.com", "d".repeat(64));
    }

    /** A cut e-mail could be the address of somebody else, so a too long one is not stored. */
    @Test
    void leavesOutAnEmailTheUsersTableCannotKeep() {
        var email = "e".repeat(243) + "@example.com";

        sync.accept(user("Jane", "Doe", email, "Jane Doe"));

        verify(users).syncUserData("jdoe", "Jane", "Doe", null, "Jane Doe");
    }

    /** A character of two code units, such as an emoji, is kept whole or left out, never cut in the middle. */
    @Test
    void doesNotCutACharacterInTheMiddle() {
        sync.accept(user("f".repeat(49) + "\uD83D\uDE00", "Doe", null, "Jane Doe"));

        verify(users).syncUserData("jdoe", "f".repeat(49), "Doe", null, "Jane Doe");
    }

    @Test
    void leavesMissingDetailsOut() {
        sync.accept(user(null, null, null, null));

        verify(users).syncUserData("jdoe", null, null, null, null);
    }

    private static SimpleUser user(String firstName, String lastName, String email, String displayName) {
        return SimpleUser.builder()
                .setUsername("jdoe")
                .setFirstName(firstName)
                .setLastName(lastName)
                .setEmail(email)
                .setDisplayName(displayName)
                .build();
    }
}
