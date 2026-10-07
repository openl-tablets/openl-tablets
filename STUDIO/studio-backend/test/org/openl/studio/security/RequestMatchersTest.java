package org.openl.studio.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Pins the addresses that are answered with data, not with a page: the REST API, which arrives as a servlet path
 * {@code /rest} and a path info, and the WebSocket handshake, which arrives as the servlet path {@code /ws} alone.
 * Pins also the files of the user guides, which arrive as the servlet path {@code /docs} and a path info.
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

    @Test
    void aPatternMatchesTheAddressesBelowItsFolder() {
        var callback = RequestMatchers.matcher("/login/oauth2/code/**");
        assertTrue(callback.matches(request("/login/oauth2/code/okta", null)));
        assertFalse(callback.matches(request("/login/other", null)));
        assertTrue(RequestMatchers.matcher("/**").matches(request("/projects", null)));
    }

    @Test
    void aFileOfTheUserGuidesIsAGuideFile() {
        assertTrue(isGuideFile("/docs", "/toc.json"));
        assertTrue(isGuideFile("/docs", "/openl-studio/index.md"));
        assertTrue(isGuideFile("/docs", "/openl-studio/images/login-page.png"));
        assertTrue(isGuideFile("/docs", "/openl-studio/missing.md"));
    }

    @Test
    void aPageOfAGuideIsNotAGuideFile() {
        assertFalse(isGuideFile("/docs", "/openl-studio/rules-editor"));
        assertFalse(isGuideFile("/docs", "/release-6.5/rules-editor"));
        assertFalse(isGuideFile("/docs", "/openl-studio/"));
        assertFalse(isGuideFile("/docs", null));
    }

    @Test
    void aQueryDoesNotTurnAPageIntoAGuideFile() {
        var request = request("/docs", "/openl-studio/rules-editor");
        request.setQueryString("image.png");
        assertFalse(RequestMatchers.userGuideFiles().matches(request));
    }

    @Test
    void aFileOutsideTheUserGuidesIsNotAGuideFile() {
        assertFalse(isGuideFile("/assets", "/index.js"));
        assertFalse(isGuideFile("/docs.png", null));
    }

    private static boolean isGuideFile(String servletPath, String pathInfo) {
        return RequestMatchers.userGuideFiles().matches(request(servletPath, pathInfo));
    }

    private static boolean isApi(String servletPath, String pathInfo) {
        return RequestMatchers.api().matches(request(servletPath, pathInfo));
    }

    private static MockHttpServletRequest request(String servletPath, String pathInfo) {
        var request = new MockHttpServletRequest("GET", servletPath + (pathInfo == null ? "" : pathInfo));
        request.setServletPath(servletPath);
        request.setPathInfo(pathInfo);
        return request;
    }
}
