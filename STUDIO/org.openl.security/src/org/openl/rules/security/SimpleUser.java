package org.openl.rules.security;

import java.util.Collection;

public class SimpleUser implements User {

    private static final long serialVersionUID = 1L;

    private String firstName;
    private String lastName;
    private String username;
    private String passwordHash;
    private Collection<Privilege> privileges;
    private String email;
    private String displayName;
    private UserExternalFlags externalFlags;

    public SimpleUser() {
    }

    public SimpleUser(String firstName,
            String lastName,
            String username,
            String passwordHash,
            Collection<Privilege> privileges,
            String email,
            String displayName,
            UserExternalFlags externalFlags) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.privileges = privileges;
        this.email = email;
        this.displayName = displayName;
        this.externalFlags = externalFlags;
    }

    public SimpleUser(String username, Collection<Privilege> privileges) {
        this.username = username;
        this.privileges = privileges;
        this.firstName = null;
        this.lastName = null;
        this.passwordHash = null;
        this.email = null;
        this.displayName = null;
        this.externalFlags = new UserExternalFlags();
    }

    @Override
    public UserExternalFlags getExternalFlags() {
        return externalFlags;
    }

    public void setExternalFlags(UserExternalFlags externalFlags) {
        this.externalFlags = externalFlags;
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
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
