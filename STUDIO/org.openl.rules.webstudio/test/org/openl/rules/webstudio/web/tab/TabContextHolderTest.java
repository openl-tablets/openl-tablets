package org.openl.rules.webstudio.web.tab;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for the request-scoped tab-context storage: empty without a bound request, round-trips within one
 * request, and clears.
 */
class TabContextHolderTest {

    @AfterEach
    void resetRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void emptyWhenNoRequestBound() {
        RequestContextHolder.resetRequestAttributes();
        assertTrue(TabContextHolder.get().isEmpty());
    }

    @Test
    void setGetClearWithinRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        assertTrue(TabContextHolder.get().isEmpty());

        var context = new TabContext("design", null, null, null, null);
        TabContextHolder.set(context);
        assertSame(context, TabContextHolder.get().orElseThrow());

        TabContextHolder.clear();
        assertTrue(TabContextHolder.get().isEmpty());
    }
}
