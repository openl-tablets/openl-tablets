package org.openl.rules.rest;

import org.openl.rules.rest.exception.ForbiddenException;
import org.springframework.security.core.GrantedAuthority;

class SecurityException extends ForbiddenException {

    private final GrantedAuthority authority;

    SecurityException(GrantedAuthority authority) {
        super("default.message");
        this.authority = authority;
    }

    public GrantedAuthority getAuthority() {
        return authority;
    }
}
