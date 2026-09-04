package org.openl.rules.security;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

public class SimpleUser implements User {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String firstName;
    @Getter
    @Setter
    private String lastName;
    @Getter
    @Setter
    private String username;
    private String passwordHash;
    private Collection<? extends GrantedAuthority> privileges;
    @Getter
    @Setter
    private String email;
    @Getter
    @Setter
    private String displayName;
    @Getter
    @Setter
    private UserExternalFlags externalFlags;
    @Getter
    @Setter
    private Instant lastLoginTime;

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
        this.lastLoginTime = other.lastLoginTime;
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
    public Collection<? extends GrantedAuthority> getAuthorities() {
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
        for (var auth : privileges) {
            if (auth.getAuthority().equals(privilege)) {
                return true;
            }

            if (auth instanceof Group group && group.hasPrivilege(privilege)) {
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
        var builder = new Builder();
        builder.setFirstName(user.firstName);
        builder.setLastName(user.lastName);
        builder.setUsername(user.username);
        builder.setPasswordHash(user.passwordHash);
        builder.setPrivileges(user.privileges);
        builder.setEmail(user.email);
        builder.setDisplayName(user.displayName);
        builder.setExternalFlags(user.externalFlags);
        builder.setLastLoginTime(user.lastLoginTime);
        return builder;
    }

    public static class Builder {

        private final SimpleUser target;

        private Builder() {
            this.target = new SimpleUser();
            this.target.externalFlags = UserExternalFlags.builder().build();
            this.target.privileges = Set.of();
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

        public Builder setPrivileges(Collection<? extends GrantedAuthority> privileges) {
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

        public Builder setLastLoginTime(Instant lastLoginTime) {
            this.target.lastLoginTime = lastLoginTime;
            return this;
        }

        public SimpleUser build() {
            return new SimpleUser(this.target);
        }
    }

}
