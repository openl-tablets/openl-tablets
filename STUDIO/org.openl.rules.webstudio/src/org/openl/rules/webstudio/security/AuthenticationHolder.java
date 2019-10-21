package org.openl.rules.webstudio.security;

import org.springframework.security.core.Authentication;

/**
 * Is intended to hold current user credentials during authentication process. Cleared after authentication process is
 * finished.
 *
 * <p>
 * Difference from SecurityContextHolder:
 * <ul>
 * <li>SecurityContextHolder holds Authentication object only <strong>after</strong> successful authentication. In some
 * cases it does not contain password (for example in AD).</li>
 * <li>AuthenticationHolder holds Authentication object <strong>during</strong> authentication process, and after it
 * does not hold. Should contain password.</li>
 * </ul>
 * </p>
 *
 * For example needed to easily access user password while filling UserDetails and user authorities in AD.
 */
public class AuthenticationHolder {
    private static final ThreadLocal<Authentication> AUTHENTICATION_HOLDER = new ThreadLocal<>();

    public static void setAuthentication(Authentication authentication) {
        AUTHENTICATION_HOLDER.set(authentication);
    }

    public static Authentication getAuthentication() {
        return AUTHENTICATION_HOLDER.get();
    }

    public static void clear() {
        AUTHENTICATION_HOLDER.remove();
    }
}
