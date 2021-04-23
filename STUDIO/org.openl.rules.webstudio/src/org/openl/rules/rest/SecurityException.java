package org.openl.rules.rest;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
class SecurityException extends RuntimeException {
    final GrantedAuthority authority;

    SecurityException(GrantedAuthority authority) {
        super("You haven't privileges to do that.");
        this.authority = authority;
    }
}
