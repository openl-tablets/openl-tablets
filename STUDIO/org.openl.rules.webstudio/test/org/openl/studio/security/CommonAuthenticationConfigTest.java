package org.openl.studio.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Pins where a visitor is sent after signing in.
 *
 * <p>The API is served by a servlet mapped at {@code /rest/*}, so a call to it arrives with {@code /rest} as
 * the servlet path and the rest as the path info — which is what the matcher reads. A screen is served by the
 * catch-all page servlet, so it arrives as a servlet path of its own.
 *
 * @author Yury Molchan
 */
class CommonAuthenticationConfigTest {

    private final CommonAuthenticationConfig config = new CommonAuthenticationConfig();

    @Test
    void aScreenIsRememberedSoTheSignInReturnsToIt() {
        assertNotNull(savedRequestFor("/projects", null), "a screen must be remembered across the sign-in");
    }

    @Test
    void anApiCallIsNotRemembered() {
        // Answering an expired session on an API call must not make the sign-in land on JSON
        // instead of a screen.
        assertNull(savedRequestFor("/rest", "/projects"), "an API call must not be remembered");
        assertNull(savedRequestFor("/rest", "/ws"), "a WebSocket handshake must not be remembered");
    }

    private Object savedRequestFor(String servletPath, String pathInfo) {
        var cache = config.httpSessionRequestCache();
        var request = new MockHttpServletRequest("GET", servletPath + (pathInfo == null ? "" : pathInfo));
        request.setServletPath(servletPath);
        request.setPathInfo(pathInfo);
        var response = new MockHttpServletResponse();
        cache.saveRequest(request, response);
        return cache.getRequest(request, response);
    }
}
