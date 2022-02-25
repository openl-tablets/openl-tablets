package org.openl.rules.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

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

    private SimpleUser(SimpleUser other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.username = other.username;
        this.passwordHash = other.passwordHash;
        this.privileges = Objects.requireNonNull(other.privileges);
        this.email = other.email;
        this.displayName = other.displayName;
        this.externalFlags = Objects.requireNonNull(other.externalFlags);
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
    public String toString() {
        return getUsername();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(SimpleUser user) {
        Builder builder = new Builder();
        builder.setFirstName(user.firstName);
        builder.setLastName(user.lastName);
        builder.setUsername(user.username);
        builder.setPasswordHash(user.passwordHash);
        builder.setPrivileges(user.privileges);
        builder.setEmail(user.email);
        builder.setDisplayName(user.displayName);
        builder.setExternalFlags(user.externalFlags);
        return builder;
    }

    public static class Builder {

        private final SimpleUser target;

        private Builder() {
            this.target = new SimpleUser();
            this.target.externalFlags = UserExternalFlags.builder().build();
            this.target.privileges = Collections.emptySet();
        }

        public Builder setFirstName(String firstName) {
            this.target.firstName = firstName;
            return this;
        }

        public Builder setLastName(String lastName) {
            this.target.lastName = lastName;
            return this;
        }

        public Builder setUsername(String username) {
            this.target.username = username;
            return this;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.target.passwordHash = passwordHash;
            return this;
        }

        public Builder setPrivileges(Collection<Privilege> privileges) {
            this.target.privileges = privileges;
            return this;
        }

        public Builder setEmail(String email) {
            this.target.email = email;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.target.displayName = displayName;
            return this;
        }

        public Builder setExternalFlags(UserExternalFlags externalFlags) {
            this.target.externalFlags = Objects.requireNonNull(externalFlags);
            return this;
        }

        public SimpleUser build() {
            return new SimpleUser(this.target);
        }
    }

}
