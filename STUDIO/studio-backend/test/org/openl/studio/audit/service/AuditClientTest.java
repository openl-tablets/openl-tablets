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

class AuditClientTest {

    private static final AuditClient NO_CLIENT = new AuditClient(null, null);

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

        assertEquals(new AuditClient("10.0.0.7", AuditAuthentication.FORM), AuditClient.current());
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

        assertEquals(List.of(new AuditClient("10.0.0.7", null), new AuditClient("10.0.0.7", null)), seen);
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
        assertEquals(new AuditClient("10.0.0.7", AuditAuthentication.FORM), AuditClient.current());
    }

    private static void request(String address) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
