package org.openl.rules.webstudio.application;

import org.openl.rules.workspace.WorkspaceUser;


/**
 * DOCUMENT ME!
 *
 * @author Andrey Naumenko
 */
public class ThreadLocalUserHolder {
    private static ThreadLocal<WorkspaceUser> userHolder = new ThreadLocal<WorkspaceUser>();

    public static WorkspaceUser getUser() {
        return userHolder.get();
    }

    public static void setUser(WorkspaceUser user) {
        userHolder.set(user);
    }
}
