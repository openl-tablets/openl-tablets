package org.openl.rules.workspace;

public class WorkspaceUserImpl implements WorkspaceUser {
    private String userId;

    public WorkspaceUserImpl(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
