package org.openl.rules.repository.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.util.StringUtils;

@RequiredArgsConstructor
public class UserInfo {
    @Getter
    private final String username;
    @Getter
    private final String email;
    private final String displayName;

    public UserInfo(String username) {
        this(username, null, null);
    }

    public String getName() {
        return StringUtils.isNotBlank(displayName) ? displayName : username;
    }
}
