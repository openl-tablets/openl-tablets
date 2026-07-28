package org.openl.rules.repository.api;

import lombok.RequiredArgsConstructor;

import org.openl.util.StringUtils;

@RequiredArgsConstructor
public class UserInfo {
    private final String username;
    private final String email;
    private final String displayName;

    public UserInfo(String username) {
        this(username, null, null);
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return StringUtils.isNotBlank(displayName) ? displayName : username;
    }
}
