package org.openl.rules.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.UserInfo;

class WorkspaceUserImplTest {

    @Test
    void decode_restores_the_user_name_the_workspace_folder_was_generated_from() {
        // The folder observed on disk must lead back to the exact principal name.
        for (var userName : new String[]{"jane", "john_doe-1", "a.b@example.com", "имя с пробелом", "we(i)rd"}) {
            var encoded = new WorkspaceUserImpl(userName, UserInfo::new).getUserId();

            assertEquals(userName, WorkspaceUserImpl.decodeUserId(encoded));
        }
    }

    @Test
    void decode_returns_a_folder_name_it_could_not_have_generated_unchanged() {
        // Arbitrary directory names come in from the disk; a malformed escape is not an encoded id.
        for (var folder : new String[]{"broken(", "empty()", "not(hex)", "tail(2e", ""}) {
            assertEquals(folder, WorkspaceUserImpl.decodeUserId(folder));
        }
    }
}
