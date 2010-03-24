package org.openl.rules.webstudio.application;

import org.openl.rules.workspace.WorkspaceUser;


/**
 * Register this bean as managed bean with application scope to get possibility to
 * access user's info in JSF EL.
 *
 * @author Andrey Naumenko
 */
public class CurrentUserInfo {
    /**
     * Currently logged in user.
     *
     * @return userInfo
     */
    public WorkspaceUser getUser() {
        WorkspaceUser user = ThreadLocalUserHolder.getUser();
        return user;
    }
}
