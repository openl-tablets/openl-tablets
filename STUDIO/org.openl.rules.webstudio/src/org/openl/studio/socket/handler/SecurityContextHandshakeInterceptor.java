package org.openl.studio.socket.handler;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Captures the security context resolved during the handshake and stores it in the WebSocket session
 * attributes, so it can be replayed on every STOMP frame by the inbound channel interceptor.
 * <p>
 * This covers the cases that are not persisted in the HTTP session:
 * <ul>
 *     <li>anonymous users on the native {@code /web/ws} endpoint, whose context is never stored in the
 *     session;</li>
 *     <li>third-party clients authenticated by an {@code Authorization} header on the {@code /rest/ws}
 *     handshake (PAT/Bearer/Basic), resolved statelessly by the {@code org.openl.studio.security} filter
 *     chains and therefore absent from the session.</li>
 * </ul>
 * When the context already comes from the HTTP session (cookie-authenticated UI), it is left untouched.
 */
public class SecurityContextHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (attributes.containsKey(SPRING_SECURITY_CONTEXT_KEY)) {
            // Security context is already provided by the HTTP session.
            return true;
        }
        var securityContext = SecurityContextHolder.getContext();
        var auth = securityContext.getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            attributes.put(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
