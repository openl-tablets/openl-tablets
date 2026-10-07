package org.openl.rules.webstudio.web.servlet;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

/**
 * The Spring dispatcher of the application.
 *
 * <p>The REST API answers under {@link #REST_PATH}, and the WebSocket handshake answers at
 * {@link #WEB_SOCKET_PATH}. They are different protocols with different callers, so each has an address of its
 * own.
 *
 * <p>Both addresses reach every handler mapping of the application context. The dispatcher asks the WebSocket
 * mapping at the WebSocket address only, and all the other mappings everywhere else. So a controller does not
 * answer at the WebSocket address, and the handshake does not answer under the REST one.
 *
 * @author Yury Molchan
 */
public class StudioDispatcherServlet extends DispatcherServlet {

    /** The prefix of the REST API. */
    public static final String REST_PATH = "/rest";

    /** The address of the WebSocket handshake. It is mapped exactly, so nothing lies below it. */
    public static final String WEB_SOCKET_PATH = "/ws";

    @Override
    protected @Nullable HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        var mappings = getHandlerMappings();
        if (mappings == null) {
            return null;
        }
        var webSocketAddress = WEB_SOCKET_PATH.equals(request.getServletPath());
        for (var mapping : mappings) {
            if ((mapping instanceof WebSocketHandlerMapping) == webSocketAddress) {
                var chain = mapping.getHandler(request);
                if (chain != null) {
                    return chain;
                }
            }
        }
        return null;
    }
}
