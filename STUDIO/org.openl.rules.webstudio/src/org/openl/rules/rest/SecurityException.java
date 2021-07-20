package org.openl.rules.rest;

import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.exception.RestRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ResponseStatus;

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
