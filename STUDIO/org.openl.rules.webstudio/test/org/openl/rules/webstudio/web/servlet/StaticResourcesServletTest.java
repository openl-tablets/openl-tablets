package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
