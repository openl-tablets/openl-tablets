package org.openl.rules.rest;

import org.springframework.security.core.GrantedAuthority;

import org.openl.rules.rest.exception.ForbiddenException;

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
