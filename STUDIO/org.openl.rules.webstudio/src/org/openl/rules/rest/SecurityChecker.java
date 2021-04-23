package org.openl.rules.rest;

import static org.openl.rules.security.AccessManager.isGranted;

import org.springframework.security.core.GrantedAuthority;

public final class SecurityChecker {
    private SecurityChecker() {
    }

    public static void allow(GrantedAuthority authority) {
        if (!isGranted(authority)) {
            throw new SecurityException(authority);
        }
    }

}
