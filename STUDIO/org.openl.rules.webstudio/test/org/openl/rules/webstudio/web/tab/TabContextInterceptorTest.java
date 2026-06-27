package org.openl.rules.webstudio.web.tab;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for the legacy-REST tab-context interceptor: it binds a resolved context for the request and
 * releases it afterwards, and does nothing when the request carries no tab identity.
 */
class TabContextInterceptorTest {

    private final TabContextInterceptor interceptor = new TabContextInterceptor();

    @AfterEach
    void resetRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @SuppressWarnings("unchecked")
    @Test
    void preHandleBindsContextAndAfterCompletionClearsIt() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        var context = new TabContext("design", null, null, null, null);

        try (var resolver = mockStatic(TabContextResolver.class)) {
            resolver.when(() -> TabContextResolver.resolve(any(Function.class))).thenReturn(context);

            boolean proceed = interceptor.preHandle(new MockHttpServletRequest(),
                    new MockHttpServletResponse(), new Object());

            assertTrue(proceed);
            assertSame(context, TabContextHolder.get().orElseThrow());

            interceptor.afterCompletion(new MockHttpServletRequest(), new MockHttpServletResponse(),
                    new Object(), null);
            assertTrue(TabContextHolder.get().isEmpty());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void preHandleLeavesNoContextWithoutIdentity() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        try (var resolver = mockStatic(TabContextResolver.class)) {
            resolver.when(() -> TabContextResolver.resolve(any(Function.class))).thenReturn(null);

            interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());
            assertTrue(TabContextHolder.get().isEmpty());
        }
    }
}
