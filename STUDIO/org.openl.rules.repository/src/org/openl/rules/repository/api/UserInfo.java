package org.openl.rules.repository.api;

import org.openl.util.StringUtils;

public class UserInfo {
    private final String username;
    private final String email;
    private final String displayName;

    public UserInfo(String username) {
        this(username, null, null);
    }

    public UserInfo(String username, String email, String displayName) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return StringUtils.isNotBlank(displayName) ? displayName : username;
    }
}
