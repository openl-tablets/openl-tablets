package org.openl.studio.security.pat.model;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

/**
 * Why a personal access token was refused.
 *
 * <p>The reason tells a wrong secret, an unknown token and a value that is no token apart, which only the audit log
 * shows. A caller learns the exact reason only once the secret matched: the token expired, or its owner cannot sign in.
 *
 * @param reason the reason, as the exception Spring Security publishes for a refused sign-in
 * @param owner  the login name of the owner of the token, or {@code null} when the token is not known
 */
public record PatRefusal(AuthenticationException reason, @Nullable String owner) {
}
