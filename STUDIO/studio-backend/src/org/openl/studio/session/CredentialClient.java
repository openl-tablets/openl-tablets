package org.openl.studio.session;

import org.springframework.security.core.Authentication;

/**
 * A client that calls OpenL Studio with credentials of its own, as {@link ClientSessions} keeps it.
 *
 * @param key            what the credentials stand for, the key its state is kept under
 * @param authentication the authentication of the request that started the client
 */
public record CredentialClient(String key, Authentication authentication) {
}
