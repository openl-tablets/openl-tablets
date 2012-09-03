package org.openl.rules.security;


public class SimpleGroup implements Group {

    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;
    private String description;
    private Privilege[] privileges;

    public SimpleGroup() {
    }

    /**
     * Construct new group
     * 
     * @param privileges nested authorities (privileges and groups)
     */
    public SimpleGroup(String name, Privilege... privileges) {
        this.name = name;
        this.privileges = privileges;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Privilege[] getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Privilege[] privileges) {
        this.privileges = privileges;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getAuthority() {
        return getName();
    }

    @Override
    public boolean hasPrivilege(String privilege) {
        return false;
    }

}
