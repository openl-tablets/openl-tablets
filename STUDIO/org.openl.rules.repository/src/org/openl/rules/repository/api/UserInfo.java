package org.openl.rules.repository.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.util.StringUtils;

/**
 * Who made a change: a value, so two reads of the same commit's author answer equal. Callers compare
 * what they read — a branch status re-read from the repository must not look like a new one.
 */
@RequiredArgsConstructor
@EqualsAndHashCode
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
