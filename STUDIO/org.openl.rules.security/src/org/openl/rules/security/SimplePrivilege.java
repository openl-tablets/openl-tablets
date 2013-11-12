package org.openl.rules.security;

import org.springframework.security.core.GrantedAuthority;

public class SimplePrivilege implements GrantedAuthority {

    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;

    public SimplePrivilege() {
    }

    public SimplePrivilege(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getAuthority() {
        return name;
    }

}
