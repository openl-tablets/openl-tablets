package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import org.openl.rules.webstudio.web.Props;

/**
 * @author Yury Molchan
 */
class FrontendPageServletTest {

    private static final String BUILT = """
            <html><head><base href="/"/>
            <link rel="modulepreload" crossorigin href="/assets/vendor-abc.js">
            <script type="module" crossorigin src="/assets/index-abc.js"></script>
            </head><body></body></html>
            """;

    private final MockServletContext pages = new MockServletContext(
            "classpath:org/openl/rules/webstudio/web/servlet/pages");
    private final MockEnvironment configuration = new MockEnvironment();

    private Environment configured;

    @BeforeEach
    void readTheConfigurationUnderTest() {
        configured = Props.getEnvironment();
        Props.setEnvironment(configuration);
    }

    @AfterEach
    void leaveTheConfigurationAsItWas() {
        Props.setEnvironment(configured);
    }

    @Test
    void theApplicationIsDrawnOnThePageTheBuildWroteForIt() throws Exception {
        var page = answerOf(new AppPageServlet(), "");

        assertTrue(page.contains("<title>OpenL Studio</title>"));
        assertTrue(page.contains("/assets/index-abc.js"));
    }

    @Test
    void theApiDocumentationIsDrawnOnAPageOfItsOwn() throws Exception {
        var page = answerOf(new ApiDocsServlet(), "");

        assertTrue(page.contains("<title>OpenL Studio API</title>"));
        assertTrue(page.contains("/assets/api-docs-abc.js"));
    }

    @Test
    void readsEveryRelativeAddressAgainstThePathTheApplicationIsDeployedUnder() throws Exception {
        var page = answerOf(new AppPageServlet(), "/webstudio");

        assertTrue(page.contains("<base href=\"/webstudio/\"/>"));
    }

    @Test
    void readsThemAgainstTheRootWhenTheApplicationIsDeployedAtIt() throws Exception {
        var page = answerOf(new AppPageServlet(), "");

        assertTrue(page.contains("<base href=\"/\"/>"));
    }

    @Test
    void isDrawnAfreshEveryTimeItIsOpened() throws Exception {
        var response = responseOf(new AppPageServlet(), "");

        assertEquals("text/html;charset=UTF-8", response.getContentType());
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void servesTheScriptsFromAFrontendDevServerWhereADeveloperRunsOne() throws Exception {
        configuration.setProperty("_REACT_UI_ROOT_", "http://localhost:3100");

        var page = answerOf(new AppPageServlet(), "");

        assertTrue(page.contains("<script type=\"module\" src=\"http://localhost:3100/src/index.tsx\"></script>"));
        assertFalse(page.contains("/assets/index-abc.js"));
    }

    @Test
    void writesThePreambleTheReactRefreshRuntimeNeeds() {
        var page = FrontendPageServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100");

        // A dollar in a replacement stands for a capturing group, so these two names once broke the whole page.
        assertTrue(page.contains("window.$RefreshReg$ = () => {}"));
        assertTrue(page.contains("window.$RefreshSig$ = () => (type) => type"));
    }

    @Test
    void servesEveryScriptFromTheDevServerAndNoneFromTheBuild() {
        var page = FrontendPageServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100");

        assertTrue(page.contains("<script type=\"module\" src=\"http://localhost:3100/@vite/client\"></script>"));
        assertTrue(page.contains("<script type=\"module\" src=\"http://localhost:3100/src/index.tsx\"></script>"));
        assertFalse(page.contains("/assets/index-abc.js"));
        assertFalse(page.contains("modulepreload"));
    }

    @Test
    void keepsTheBaseSoRelativeAddressesStillResolve() {
        var page = FrontendPageServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100");

        assertTrue(page.contains("<base href=\"/\"/>"));
    }

    @Test
    void readsTheDevServerAddressWithOrWithoutATrailingSlash() {
        var withSlash = FrontendPageServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100/");

        assertEquals(FrontendPageServlet.servedFrom(BUILT, "/src/index.tsx", "http://localhost:3100"), withSlash);
    }

    @Test
    void answersWithAnErrorWhenThePageCannotBeWritten() throws Exception {
        var servlet = new AppPageServlet();
        servlet.init(new MockServletConfig(pages));
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new IOException("boom"));

        servlet.service(new MockHttpServletRequest(pages, "GET", "/"), response);

        verify(response).reset();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private String answerOf(HttpServlet servlet, String contextPath) throws Exception {
        return responseOf(servlet, contextPath).getContentAsString();
    }

    private MockHttpServletResponse responseOf(HttpServlet servlet, String contextPath) throws Exception {
        servlet.init(new MockServletConfig(pages));
        var request = new MockHttpServletRequest(pages, "GET", contextPath + "/");
        request.setContextPath(contextPath);
        var response = new MockHttpServletResponse();

        servlet.service(request, response);

        return response;
    }
}
