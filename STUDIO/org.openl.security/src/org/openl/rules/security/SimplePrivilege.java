package org.openl.rules.security;

public class SimplePrivilege implements Privilege {

    private static final long serialVersionUID = 2L;

    private final String name;

    public SimplePrivilege(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getAuthority() {
        return name;
    }

}
