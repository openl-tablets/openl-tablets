package org.openl.rules.security;

import java.util.Collection;

public class SimpleGroup implements Group {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private Collection<Privilege> privileges;

    public SimpleGroup() {
    }

    /**
     * Construct new group
     *
     * @param privileges nested authorities (privileges and groups)
     */
    public SimpleGroup(String name, String description, Collection<Privilege> privileges) {
        this.name = name;
        this.description = description;
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
    public Collection<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Collection<Privilege> privileges) {
        this.privileges = privileges;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getAuthority() {
        return getName();
    }

    @Override
    public boolean hasPrivilege(String privilege) {
        for (Privilege auth : privileges) {
            if (auth.getName().equals(privilege)) {
                return true;
            }

            if (auth instanceof Group) {
                if (((Group) auth).hasPrivilege(privilege)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean hasGroup(String groupName) {
        for (Privilege privilege : privileges) {
            if (privilege instanceof Group) {
                Group x = (Group) privilege;
                if (x.getName().equalsIgnoreCase(groupName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

}
