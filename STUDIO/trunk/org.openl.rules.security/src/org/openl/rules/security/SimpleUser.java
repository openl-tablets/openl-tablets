package org.openl.rules.security;

import java.util.Collection;

public class SimpleUser implements User {

    private static final long serialVersionUID = 1L;

    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private Collection<Privilege> privileges;

    public SimpleUser() {
    }

    public SimpleUser(String firstName, String lastName, String username,
            String password, Collection<Privilege> privileges) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.privileges = privileges;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Collection<Privilege> getAuthorities() {
        return privileges;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
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
    public String toString() {
        return getUsername();
    }

}
