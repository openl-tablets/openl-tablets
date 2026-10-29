package org.openl.studio.security.pat.model;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Refuses an {@code Authorization: Token} header that holds no personal access token at all.
 */
public class MalformedPatException extends BadCredentialsException {

    public MalformedPatException() {
        super("The value is not a personal access token");
    }
}
