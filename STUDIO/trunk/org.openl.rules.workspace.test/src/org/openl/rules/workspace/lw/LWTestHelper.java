package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;

public class LWTestHelper {
    private static final WorkspaceUser USER_TEST = new WorkspaceUserImpl("test");

    public static WorkspaceUser getTestUser() {
        return USER_TEST;
    }
}
