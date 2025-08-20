package org.openl.rules.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

public class SimpleGroup implements Group {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private Collection<GrantedAuthority> privileges;

    public SimpleGroup() {
    }

    /**
     * Construct new group
     *
     * @param privileges nested authorities (privileges and groups)
     */
    public SimpleGroup(String name, String description, Collection<GrantedAuthority> privileges) {
        this.name = name;
        this.description = description;
        this.privileges = privileges;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Collection<GrantedAuthority> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Collection<GrantedAuthority> privileges) {
        this.privileges = privileges;
    }

    @Override
    public String getAuthority() {
        return name;
    }

    @Override
    public boolean hasPrivilege(String privilege) {
        for (var auth : privileges) {
            if (auth.getAuthority().equals(privilege)) {
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
    public String toString() {
        return name;
    }

}
