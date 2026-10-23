package org.openl.studio.transfer;

import java.util.Collection;

import lombok.EqualsAndHashCode;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authentication of a request to a transfer link: the user who asked for the link, with the authorities the
 * user has when the link is used.
 *
 * <p>It is no {@code UsernamePasswordAuthenticationToken}, so nothing takes a request to a link for a sign-in with a
 * password.
 *
 * @author Yury Molchan
 */
@EqualsAndHashCode(callSuper = true)
public class TransferLinkAuthentication extends AbstractAuthenticationToken {

    private final transient UserDetails user;

    public TransferLinkAuthentication(UserDetails user, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        setAuthenticated(true);
    }

    @Override
    public UserDetails getPrincipal() {
        return user;
    }

    @Override
    public Object getCredentials() {
        return "";
    }
}
