package org.openl.rules.security.integration.ipb.dataload;

import com.exigen.epb.security.model.User;

/**
 * @author Aliaksandr Antonik.
*/
public class UserPrivilegeInfo {
    private User user;
    private String[] privileges;

    public void setPrivileges(String[] privileges) {
        this.privileges = privileges;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String[] getPrivileges() {
        return privileges;
    }

    public User getUser() {
        return user;
    }
}
