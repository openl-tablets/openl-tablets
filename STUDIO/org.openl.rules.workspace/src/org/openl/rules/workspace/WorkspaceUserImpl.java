package org.openl.rules.workspace;

public class WorkspaceUserImpl implements WorkspaceUser {
    private String userId;
    private String userName;

    public WorkspaceUserImpl(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

}
