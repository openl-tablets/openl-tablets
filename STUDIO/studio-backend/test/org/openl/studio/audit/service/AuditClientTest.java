package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.mcp.McpCallsAccess;

class AuditClientTest {

    private static final AuditClient NO_CLIENT = AuditClient.builder().build();
    /** The client of a request from 10.0.0.7 of a user signed in with the login form. */
    private static final AuditClient FORM_CLIENT = AuditClient.builder()
            .address("10.0.0.7")
            .authentication(AuditAuthentication.FORM)
            .build();
    /** The client of a request from 10.0.0.7 that tells nothing of a sign-in. */
    private static final AuditClient ADDRESS_ONLY = AuditClient.builder().address("10.0.0.7").build();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void namesTheClientOfTheCurrentRequest() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("jdoe",
                null,
                List.of()));
        request("10.0.0.7");

        assertEquals(FORM_CLIENT, AuditClient.current());
    }

    @Test
    void namesTheClientOfTheMcpServerForACallTheServerMakes() {
        var request = request("10.0.0.5");
        request.addHeader("OpenL-Mcp-Secret", McpCallsAccess.newSecret());
        request.addHeader("OpenL-Client-Address", "203.0.113.7");

        assertEquals("203.0.113.7", AuditClient.current().address());
    }

    @Test
    void namesTheAddressARequestComesFromWhenItClaimsAnotherOne() {
        McpCallsAccess.newSecret();
        var request = request("10.0.0.5");
        request.addHeader("OpenL-Mcp-Secret", "guessed");
        request.addHeader("OpenL-Client-Address", "203.0.113.7");

        assertEquals("10.0.0.5", AuditClient.current().address());
    }

    @Test
    void namesNoClientOutsideARequest() {
        assertEquals(NO_CLIENT, AuditClient.current());
    }

    @Test
    void aTaskNamesTheClientOfTheRequestThatStartedItAndLeavesNoneBehind() throws InterruptedException {
        request("10.0.0.7");
        var seen = new ArrayList<AuditClient>();
        var task = AuditClient.carry(() -> seen.add(AuditClient.current()));
        RequestContextHolder.resetRequestAttributes();

        Thread.ofVirtual().start(task).join();
        task.run();

        assertEquals(List.of(ADDRESS_ONLY, ADDRESS_ONLY), seen);
        assertEquals(NO_CLIENT, AuditClient.current());
    }

    @Test
    void aTaskRunInsideAnotherOneGivesItsClientBack() {
        var seen = new ArrayList<String>();
        request("10.0.0.8");
        var inner = AuditClient.carry(() -> seen.add(AuditClient.current().address()));
        request("10.0.0.7");
        var outer = AuditClient.carry(() -> {
            inner.run();
            seen.add(AuditClient.current().address());
        });
        RequestContextHolder.resetRequestAttributes();

        outer.run();

        assertEquals(List.of("10.0.0.8", "10.0.0.7"), seen);
    }

    @Test
    void anActionOfOpenLStudioNamesNoClientEvenOnTheThreadOfARequest() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("jdoe",
                null,
                List.of()));
        request("10.0.0.7");
        var seen = new ArrayList<AuditClient>();

        AuditClient.runAsApplication(() -> seen.add(AuditClient.current()));

        assertEquals(List.of(NO_CLIENT), seen);
        assertEquals(FORM_CLIENT, AuditClient.current());
    }

    private static MockHttpServletRequest request(String address) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }
}
