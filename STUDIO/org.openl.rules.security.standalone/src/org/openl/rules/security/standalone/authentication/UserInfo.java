package org.openl.rules.security.standalone.authentication;

import java.util.Collection;

import org.openl.rules.security.standalone.model.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Contains information about user that works with application. This class
 * contains all fields that are specified by
 * {@link org.springframework.security.core.userdetails.UserDetails} interface.
 *
 * @author Andrey Naumenko
 */
public class UserInfo implements UserDetails {

    private static final long serialVersionUID = 1L;

    private User user;
    private Collection<GrantedAuthority> grantedAuthorities;

    public UserInfo() {
    }

    public UserInfo(User user, Collection<GrantedAuthority> grantedAuthorities) {
        this.user = user;
        this.grantedAuthorities = grantedAuthorities;
    }

    public Collection<GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    public String getPassword() {
        return user.getPassword();
    }

    public User getUser() {
        return user;
    }

    public String getUsername() {
        if (user == null) {
            return null;
        }
        return user.getLoginName();
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return getUsername();
    }
}
