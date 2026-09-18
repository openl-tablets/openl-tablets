package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import jakarta.servlet.ServletException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link CrossSiteRequestFilter}. The trust list is injected through
 * {@link CrossSiteRequestFilter#configure} so the decision runs without a servlet container.
 *
 * @author Yury Molchan
 */
class CrossSiteRequestFilterTest {

    private CrossSiteRequestFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CrossSiteRequestFilter();
        filter.configure("https://trusted.example, https://app.example.com");
    }

    private static MockHttpServletRequest request(String method) {
        var request = new MockHttpServletRequest(method, "/rest/projects/AA/files/x.xlsx");
        request.setServerName("studio.example.com");
        request.setServerPort(443);
        request.setScheme("https");
        return request;
    }

    /** A {@code POST} to the address above, with each header that is not {@code null}. */
    private static MockHttpServletRequest post(@Nullable String secFetchSite, @Nullable String origin) {
        var request = request("POST");
        if (secFetchSite != null) {
            request.addHeader("Sec-Fetch-Site", secFetchSite);
        }
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }

    private MockHttpServletResponse run(MockHttpServletRequest request, MockFilterChain chain) throws IOException,
            ServletException {
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void safeMethodPassesEvenWhenCrossSite() throws Exception {
        var request = request("GET");
        request.addHeader("Sec-Fetch-Site", "cross-site");
        request.addHeader("Origin", "https://evil.example");
        var chain = new MockFilterChain();

        var response = run(request, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest(name = "Sec-Fetch-Site: {0}, Origin: {1}")
    @CsvSource(delimiter = '|', nullValues = "-", textBlock = """
            # The application's own page, and a request the user started, such as typing the address.
            same-origin | -
            none        | -
            # Sec-Fetch-Site decides over an Origin a proxy that rewrites the host left unmatched.
            same-origin | https://studio.public.example
            # An origin of the trust list, in any case.
            cross-site  | https://trusted.example
            cross-site  | HTTPS://Trusted.Example
            # A client that sends no Sec-Fetch-Site: a non-browser client, the very host, a listed origin.
            -           | -
            -           | https://studio.example.com
            -           | https://studio.example.com:443
            -           | https://trusted.example
            """)
    void statefulRequestPasses(@Nullable String secFetchSite, @Nullable String origin) throws Exception {
        var chain = new MockFilterChain();

        run(post(secFetchSite, origin), chain);

        assertNotNull(chain.getRequest());
    }

    @ParameterizedTest(name = "Sec-Fetch-Site: {0}, Origin: {1}")
    @CsvSource(delimiter = '|', nullValues = "-", textBlock = """
            # Sec-Fetch-Site decides even when Origin names the very host.
            cross-site  | https://studio.example.com
            same-site   | https://sibling.example.com
            # A client that sends no Sec-Fetch-Site: another host, another port, an opaque or malformed origin.
            -           | https://evil.example
            -           | https://studio.example.com:8443
            -           | null
            -           | studio.example.com
            -           | http://[bad
            """)
    void statefulRequestIsRejected(@Nullable String secFetchSite, @Nullable String origin) throws Exception {
        var chain = new MockFilterChain();

        var response = run(post(secFetchSite, origin), chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void crossSiteUntrustedIsRejected() throws Exception {
        var chain = new MockFilterChain();

        var response = run(post("cross-site", "https://evil.example"), chain);

        assertEquals(403, response.getStatus());
        assertEquals("text/plain;charset=UTF-8", response.getContentType());
        assertEquals("Cross-site request rejected.", response.getContentAsString());
        assertNull(chain.getRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void everyStateChangingMethodIsGuarded(String method) throws Exception {
        var request = request(method);
        request.addHeader("Sec-Fetch-Site", "cross-site");
        request.addHeader("Origin", "https://evil.example");
        var chain = new MockFilterChain();

        var response = run(request, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void wildcardTrustsEveryOrigin() throws Exception {
        filter.configure("*");
        var chain = new MockFilterChain();

        run(post("cross-site", "https://evil.example"), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void emptyTrustListStillRejectsCrossSite() throws Exception {
        filter.configure("");
        var chain = new MockFilterChain();

        var response = run(post("cross-site", "https://evil.example"), chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void absentTrustListIsHandledWithoutError() throws Exception {
        filter.configure(null);
        var chain = new MockFilterChain();

        run(post("same-origin", null), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void httpsOriginMatchesRequestSeenAsPlainHttp() throws Exception {
        var request = post(null, "https://studio.example.com");
        request.setScheme("http");
        request.setServerPort(80);
        var chain = new MockFilterChain();

        run(request, chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void originWithMatchingNonDefaultPortPasses() throws Exception {
        var request = post(null, "http://studio.example.com:8080");
        request.setScheme("http");
        request.setServerPort(8080);
        var chain = new MockFilterChain();

        run(request, chain);

        assertNotNull(chain.getRequest());
    }
}
