package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import org.openl.rules.webstudio.web.Props;

/**
 * @author Yury Molchan
 */
class StaticResourcesServletTest {

    private static final String BUILT = """
            <html><head><base href="/"/>
            <link rel="modulepreload" crossorigin href="/assets/vendor-abc.js">
            <script type="module" crossorigin src="/assets/index-abc.js"></script>
            </head><body></body></html>
            """;

    private ServletContext servletContext;
    private RequestDispatcher defaultServlet;
    private StaticResourcesServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        Props.setEnvironment(new MockEnvironment());
        servletContext = mock(ServletContext.class);
        defaultServlet = mock(RequestDispatcher.class);
        when(servletContext.getNamedDispatcher("default")).thenReturn(defaultServlet);
        when(servletContext.getResourceAsStream("/index.html")).thenReturn(page());
        when(servletContext.getResourceAsStream("/api-docs.html")).thenReturn(page());

        servlet = new StaticResourcesServlet();
        servlet.init(new MockServletConfig(servletContext, "static"));
    }

    @AfterEach
    void tearDown() {
        Props.setEnvironment(null);
    }

    @Test
    void drawsThePageWithTheBaseTheApplicationIsRunningAt() throws Exception {
        var request = new MockHttpServletRequest(servletContext, "GET", "/studio");
        request.setContextPath("/studio");
        var response = new MockHttpServletResponse();

        servlet.service(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("<base href=\"/studio/\"/>"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void handsAFileOfTheBuildToTheContainer() throws Exception {
        var request = new MockHttpServletRequest(servletContext, "GET", "/assets/index-abc.js");
        request.setPathInfo("/assets/index-abc.js");
        var response = new MockHttpServletResponse();

        servlet.service(request, response);

        verify(defaultServlet).forward(any(), any());
    }

    @Test
    void answersWithAnErrorWhenTheContainerCannotServeTheFile() throws Exception {
        doThrow(new ServletException("boom")).when(defaultServlet).forward(any(), any());
        var request = new MockHttpServletRequest(servletContext, "GET", "/assets/index-abc.js");
        request.setPathInfo("/assets/index-abc.js");
        var response = mock(HttpServletResponse.class);

        servlet.service(request, response);

        verify(response).reset();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private static ByteArrayInputStream page() {
        return new ByteArrayInputStream(BUILT.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void writesThePreambleTheReactRefreshRuntimeNeeds() {
        var page = StaticResourcesServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100");

        // A dollar in a replacement stands for a capturing group, so these two names once broke the whole page.
        assertTrue(page.contains("window.$RefreshReg$ = () => {}"));
        assertTrue(page.contains("window.$RefreshSig$ = () => (type) => type"));
    }

    @Test
    void servesEveryScriptFromTheDevServerAndNoneFromTheBuild() {
        var page = StaticResourcesServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100");

        assertTrue(page.contains("<script type=\"module\" src=\"http://localhost:3100/@vite/client\"></script>"));
        assertTrue(page.contains("<script type=\"module\" src=\"http://localhost:3100/src/index.tsx\"></script>"));
        assertFalse(page.contains("/assets/index-abc.js"));
        assertFalse(page.contains("modulepreload"));
    }

    @Test
    void keepsTheBaseSoRelativeAddressesStillResolve() {
        var page = StaticResourcesServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100");

        assertTrue(page.contains("<base href=\"/\"/>"));
    }

    @Test
    void readsTheDevServerAddressWithOrWithoutATrailingSlash() {
        var withSlash = StaticResourcesServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100/");

        assertEquals(StaticResourcesServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100"), withSlash);
    }
}
