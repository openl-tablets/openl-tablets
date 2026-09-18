package org.openl.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link CorsPolicy}.
 *
 * @author Yury Molchan
 */
class CorsPolicyTest {

    private static final String ORIGIN = "https://app.example.com";

    private final CorsPolicy policy = policy(ORIGIN + ", https://other.example", "GET,POST",
            "Content-Type,Authorization", "X-Id", 600L);

    private static CorsPolicy policy(String origins, String methods, String headers, String expose, Long maxAge) {
        return new CorsPolicy(AllowedOrigins.parse(origins), methods, headers, expose, maxAge);
    }

    private static MockHttpServletRequest request(String method, String origin) {
        var request = new MockHttpServletRequest(method, "/rest/users");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }

    private static MockHttpServletRequest preflight(String origin) {
        var request = request("OPTIONS", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        return request;
    }

    @Test
    void listedOriginIsEchoedOnAnActualResponse() {
        var response = new MockHttpServletResponse();

        assertFalse(policy.handle(request("GET", ORIGIN), response));

        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals(List.of("Origin"), response.getHeaders("Vary"));
        assertNull(response.getHeader("Access-Control-Allow-Methods"));
        assertNull(response.getHeader("Access-Control-Allow-Headers"));
        assertEquals("X-Id", response.getHeader("Access-Control-Expose-Headers"));
        assertNull(response.getHeader("Access-Control-Max-Age"));
    }

    @Test
    void preflightOfListedOriginIsAnsweredInFull() {
        var response = new MockHttpServletResponse();

        assertTrue(policy.handle(preflight(ORIGIN), response));

        assertEquals(204, response.getStatus());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("GET,POST", response.getHeader("Access-Control-Allow-Methods"));
        assertEquals("Content-Type,Authorization", response.getHeader("Access-Control-Allow-Headers"));
        assertEquals("600", response.getHeader("Access-Control-Max-Age"));
        assertEquals(List.of("Origin"), response.getHeaders("Vary"));
    }

    @Test
    void optionsWithoutRequestedMethodIsNotAPreflight() {
        var response = new MockHttpServletResponse();

        assertFalse(policy.handle(request("OPTIONS", ORIGIN), response));

        assertEquals(200, response.getStatus());
        assertEquals(ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Methods"));
    }

    @Test
    void unlistedOriginGetsNoAllowHeaderButStillVaries() {
        var response = new MockHttpServletResponse();

        assertFalse(policy.handle(preflight("https://evil.example"), response));

        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertEquals(List.of("Origin"), response.getHeaders("Vary"));
    }

    @Test
    void requestWithoutOriginStillVariesWhenAnOriginIsListed() {
        var response = new MockHttpServletResponse();

        assertFalse(policy.handle(request("POST", null), response));

        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertEquals(List.of("Origin"), response.getHeaders("Vary"));
    }

    @Test
    void emptySettingAddsNoHeaderAtAll() {
        var response = new MockHttpServletResponse();

        assertFalse(policy(null, "GET,POST", "Content-Type", null, 600L).handle(preflight(ORIGIN), response));

        assertTrue(response.getHeaderNames().isEmpty());
    }

    @Test
    void asteriskEchoesEveryOrigin() {
        var response = new MockHttpServletResponse();

        policy("*", "GET,POST", "Content-Type", null, 600L).handle(request("GET", "https://any.example"), response);

        assertEquals("https://any.example", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals(List.of("Origin"), response.getHeaders("Vary"));
    }

    @Test
    void listedOriginIsEchoedAsTheBrowserSpelledIt() {
        var response = new MockHttpServletResponse();

        policy.handle(request("GET", "HTTPS://App.Example.com"), response);

        assertEquals("HTTPS://App.Example.com", response.getHeader("Access-Control-Allow-Origin"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1, -7200})
    void missingOrNegativeCacheTimeLeavesMaxAgeOut(Long maxAge) {
        var response = new MockHttpServletResponse();

        assertTrue(policy(ORIGIN, "GET", "Content-Type", null, maxAge).handle(preflight(ORIGIN), response));

        assertEquals(204, response.getStatus());
        assertNull(response.getHeader("Access-Control-Max-Age"));
        assertEquals("GET", response.getHeader("Access-Control-Allow-Methods"));
    }

    @Test
    void blankMethodAndHeaderListsLeaveTheirHeadersOut() {
        var response = new MockHttpServletResponse();

        policy(ORIGIN, " ", null, " ", 600L).handle(preflight(ORIGIN), response);

        assertNull(response.getHeader("Access-Control-Allow-Methods"));
        assertNull(response.getHeader("Access-Control-Allow-Headers"));
        assertEquals("600", response.getHeader("Access-Control-Max-Age"));
    }

    @Test
    void settingsAreReadFromTheEnvironment() {
        var settings = new MockEnvironment().withProperty("cors.allowed.origins", " * ")
                .withProperty("cors.allowed.methods", "GET")
                .withProperty("cors.allowed.headers", "X-Id")
                .withProperty("cors.exposed.headers", "X-Out")
                .withProperty("cors.preflight.maxage", "-1");

        var read = CorsPolicy.read(settings);

        assertTrue(read.allowedOrigins().any());
        assertEquals("GET", read.allowedMethods());
        assertEquals("X-Id", read.allowedHeaders());
        assertEquals("X-Out", read.exposedHeaders());
        assertNull(read.maxAge());
    }

    @Test
    void missingOrBlankSettingsReadAsNone() {
        var read = CorsPolicy.read(new MockEnvironment().withProperty("cors.preflight.maxage", ""));

        assertTrue(read.allowedOrigins().isEmpty());
        assertNull(read.allowedMethods());
        assertNull(read.allowedHeaders());
        assertNull(read.maxAge());
    }
}
