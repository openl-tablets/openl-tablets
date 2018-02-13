package org.openl.rules.security;

public class SimplePrivilege implements Privilege {

    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;

    public SimplePrivilege() {
    }

    public SimplePrivilege(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
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
