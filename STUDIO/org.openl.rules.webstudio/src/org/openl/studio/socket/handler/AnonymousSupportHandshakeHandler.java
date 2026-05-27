package org.openl.studio.socket.handler;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Resolves the WebSocket session {@link Principal} so messages can be routed to a specific user session
 * instead of being broadcast.
 * <p>
 * {@link DefaultHandshakeHandler#determineUser} relies on {@code request.getUserPrincipal()}, which is
 * {@code null} for anonymous users and for the manually-assembled {@code /rest/**} filter chains (they do
 * not install the servlet security wrapper). In those cases the principal is taken from the current
 * {@link SecurityContextHolder}, so anonymous {@code /web/ws} sessions and header-authenticated
 * ({@code /rest/ws}) sessions both get a routable principal.
 */
public class AnonymousSupportHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        return resolveUser(super.determineUser(request, wsHandler, attributes));
    }

    /**
     * Falls back to the current security context when the servlet request exposes no principal. Extracted as a
     * static helper so it can be unit tested without a servlet/WebSocket runtime.
     *
     * @param servletPrincipal the principal resolved by {@link DefaultHandshakeHandler}, may be {@code null}
     * @return the resolved principal, or {@code null} when neither source provides an authenticated user
     */
    static Principal resolveUser(Principal servletPrincipal) {
        if (servletPrincipal != null) {
            return servletPrincipal;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth;
        }
        return null;
    }
}
