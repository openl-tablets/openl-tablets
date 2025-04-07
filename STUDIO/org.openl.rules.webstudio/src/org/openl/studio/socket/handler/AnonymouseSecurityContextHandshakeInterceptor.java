package org.openl.studio.socket.handler;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Store security context to attributes to be able to use it in the websocket session.
 * It is necessary for anonymous users as their security context is not stored in the HTTP session.
 */
public class AnonymouseSecurityContextHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthenticationTrustResolver trustResolver;

    public AnonymouseSecurityContextHandshakeInterceptor() {
        this.trustResolver = new AuthenticationTrustResolverImpl();
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        if (attributes.containsKey(SPRING_SECURITY_CONTEXT_KEY)) {
            // Security context is already stored in attributes.
            return true;
        }
        var securityContext = SecurityContextHolder.getContext();
        var auth = securityContext.getAuthentication();
        if (trustResolver.isAnonymous(auth)) {
            attributes.put(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {

    }
}
