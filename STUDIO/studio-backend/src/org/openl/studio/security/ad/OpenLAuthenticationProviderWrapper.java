package org.openl.studio.security.ad;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

@RequiredArgsConstructor
public class OpenLAuthenticationProviderWrapper implements AuthenticationProvider {
    private final AuthenticationProvider delegate;

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!delegate.supports(authentication.getClass())) {
            return null;
        }

        try {
            AuthenticationHolder.setAuthentication(authentication);

            return delegate.authenticate(authentication);
        } finally {
            AuthenticationHolder.clear();
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && Authentication.class.isAssignableFrom(authentication);
    }
}
