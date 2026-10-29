package org.openl.studio.security.pat.model;

import org.springframework.security.authentication.CredentialsExpiredException;

/**
 * Refuses a personal access token that has expired. It is told only to a caller who sent the right secret.
 */
public class ExpiredPatException extends CredentialsExpiredException {

    public ExpiredPatException() {
        super("The personal access token has expired");
    }
}
