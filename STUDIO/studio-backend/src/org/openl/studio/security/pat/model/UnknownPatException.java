package org.openl.studio.security.pat.model;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Refuses a personal access token OpenL Studio does not know, such as a deleted one.
 */
public class UnknownPatException extends BadCredentialsException {

    public UnknownPatException() {
        super("The personal access token is unknown");
    }
}
