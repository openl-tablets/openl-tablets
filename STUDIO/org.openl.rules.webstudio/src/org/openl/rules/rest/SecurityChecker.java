package org.openl.rules.rest;

import static org.openl.rules.security.AccessManager.isGranted;

import org.springframework.security.core.GrantedAuthority;

class SecurityChecker {

    static void allow(GrantedAuthority authority) {
        if (!isGranted(authority)) {
            throw new SecurityException(authority);
        }
    }

}
