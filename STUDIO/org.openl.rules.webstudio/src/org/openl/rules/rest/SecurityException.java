package org.openl.rules.rest;

import org.springframework.security.core.GrantedAuthority;

class SecurityException extends RuntimeException {
    final GrantedAuthority authority;

    SecurityException(GrantedAuthority authority) {
        super();
        this.authority = authority;
    }
}
