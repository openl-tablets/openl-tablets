package org.openl.studio.socket.handler;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Set anonymous user (Principal) in WebSocket messages
 * This is necessary to avoid broadcasting messages but sending them to specific user sessions
 */
public class AnonymousSupportHandshakeHandler extends DefaultHandshakeHandler {

    private final AuthenticationTrustResolver trustResolver;

    public AnonymousSupportHandshakeHandler() {
        this.trustResolver = new AuthenticationTrustResolverImpl();
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var principal =  super.determineUser(request, wsHandler, attributes);
        if (principal == null) {
            var securityContext = SecurityContextHolder.getContext();
            var auth = securityContext.getAuthentication();
            if (trustResolver.isAnonymous(auth)) {
                principal = auth;
            }
        }
        return principal;
    }
}
