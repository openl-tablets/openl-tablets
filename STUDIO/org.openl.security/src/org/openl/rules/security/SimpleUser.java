package org.openl.rules.security;

import java.util.Collection;

public class SimpleUser implements User {

    private static final long serialVersionUID = 1L;

    private String firstName;
    private String lastName;
    private String username;
    private String passwordHash;
    private Collection<Privilege> privileges;

    public SimpleUser() {
    }

    public SimpleUser(String firstName,
            String lastName,
            String username,
            String passwordHash,
            Collection<Privilege> privileges) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.passwordHash = passwordHash;
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

    /**
     * In this implementation returns the password hash
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String passwordHash) {
        this.passwordHash = passwordHash;
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

            if (auth instanceof Group && ((Group) auth).hasPrivilege(privilege)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isInternalUser() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    @Override
    public String toString() {
        return getUsername();
    }

}
