package org.openl.studio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Checks that a refused single sign-on is started again once, and refused for good the second time.
 *
 * @author Yury Molchan
 */
class RetryOnceAuthenticationFailureHandlerTest {

    private final RetryOnceAuthenticationFailureHandler handler = new RetryOnceAuthenticationFailureHandler(
            "/oauth2/authorization/webstudio");
    private final MockHttpSession session = new MockHttpSession();

    @Test
    void startsTheSignInAgainAfterTheFirstRefusal() throws Exception {
        var response = fail();

        assertEquals(302, response.getStatus());
        assertEquals("/webstudio/oauth2/authorization/webstudio", response.getRedirectedUrl());
    }

    @Test
    void refusesTheSecondAttemptAndLetsTheNextOneRetryAgain() throws Exception {
        fail();

        var second = fail();
        assertEquals(401, second.getStatus());
        assertNull(second.getRedirectedUrl());

        // The user starts over: the attempt after a final refusal is repeated once again.
        assertEquals(302, fail().getStatus());
    }

    private MockHttpServletResponse fail() throws Exception {
        var request = new MockHttpServletRequest("GET", "/webstudio/login/oauth2/code/webstudio");
        request.setContextPath("/webstudio");
        request.setSession(session);
        var response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("authorization_request_not_found"));
        return response;
    }
}
