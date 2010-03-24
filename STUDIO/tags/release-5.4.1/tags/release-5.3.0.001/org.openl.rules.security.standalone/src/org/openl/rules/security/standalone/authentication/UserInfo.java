package org.openl.rules.security.standalone.authentication;

import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.GrantedAuthority;
import org.openl.rules.security.standalone.model.User;

/**
 * Contains information about user that works with application. This class
 * contains all fields that are specified by
 * {@link org.acegisecurity.userdetails.UserDetails} interface.
 *
 * @author Andrey Naumenko
 */
public class UserInfo implements UserDetails {
    private static final long serialVersionUID = 1L;
    private User user;
    private GrantedAuthority[] grantedAuthorities;

    public UserInfo() {
    }

    public UserInfo(User user, GrantedAuthority[] grantedAuthorities) {
        this.user = user;
        this.grantedAuthorities = grantedAuthorities;
    }

    public GrantedAuthority[] getAuthorities() {
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
