package org.openl.studio.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Pins the addresses that are answered with data, not with a page: the REST API, which arrives as a servlet path
 * {@code /rest} and a path info, and the WebSocket handshake, which arrives as the servlet path {@code /ws} alone.
 *
 * @author Yury Molchan
 */
class RequestMatchersTest {

    @Test
    void theRestApiIsAnApiAddress() {
        assertTrue(isApi("/rest", "/projects"));
        assertTrue(isApi("/rest", "/projects/1/files"));
    }

    @Test
    void theWebSocketHandshakeIsAnApiAddress() {
        assertTrue(isApi("/ws", null));
    }

    @Test
    void aScreenIsNotAnApiAddress() {
        assertFalse(isApi("/projects", null));
        assertFalse(isApi("/login", null));
        assertFalse(isApi("/assets", "/index.js"));
    }

    @Test
    void anAddressBesideTheWebSocketOneIsNotAnApiAddress() {
        assertFalse(isApi("/wss", null));
        assertFalse(isApi("/ws", "/other"));
    }

    private static boolean isApi(String servletPath, String pathInfo) {
        var request = new MockHttpServletRequest("GET", servletPath + (pathInfo == null ? "" : pathInfo));
        request.setServletPath(servletPath);
        request.setPathInfo(pathInfo);
        return RequestMatchers.api().matches(request);
    }
}
