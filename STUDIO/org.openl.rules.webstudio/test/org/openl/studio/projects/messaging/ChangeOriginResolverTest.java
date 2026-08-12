package org.openl.studio.projects.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ChangeOriginResolverTest {

    private final ChangeOriginResolver resolver = new ChangeOriginResolver();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void requestWithOrigin(String origin) {
        var request = new MockHttpServletRequest();
        request.addHeader(ChangeOriginResolver.ORIGIN_HEADER, origin);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void names_the_client_the_request_came_from() {
        requestWithOrigin("9f1c3b7e-0f5d-4b1a-9a7c-2f0d5c8e6b41");

        assertEquals("9f1c3b7e-0f5d-4b1a-9a7c-2f0d5c8e6b41", resolver.current());
    }

    @Test
    void a_change_made_outside_a_request_has_no_origin() {
        // The files watcher and the repository polling publish from their own threads.
        assertNull(resolver.current());
    }

    @Test
    void a_request_that_named_no_client_has_no_origin() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertNull(resolver.current());
    }

    @Test
    void an_id_that_is_not_an_opaque_token_is_ignored() {
        // Nothing unbounded or unexpected reaches the subscribers of a ping.
        requestWithOrigin("a".repeat(65));
        assertNull(resolver.current());

        requestWithOrigin("tab 1");
        assertNull(resolver.current());

        requestWithOrigin("");
        assertNull(resolver.current());
    }
}
