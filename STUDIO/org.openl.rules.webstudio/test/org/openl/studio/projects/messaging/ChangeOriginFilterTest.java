package org.openl.studio.projects.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import org.openl.rules.webstudio.web.servlet.SpringInitializer;

class ChangeOriginFilterTest {

    private final MockServletContext servletContext = new MockServletContext();
    private final ChangeOriginFilter filter = new ChangeOriginFilter();
    private final ChangeOriginResolver changeOrigin = publishResolver();

    private void handle(String method, String uri) throws Exception {
        filter.doFilter(new MockHttpServletRequest(servletContext, method, uri),
                new MockHttpServletResponse(),
                new MockFilterChain());
    }

    @Test
    void marks_the_client_of_a_change_as_a_recent_writer() throws Exception {
        handle("PATCH", "/web/projects/p1");

        // Twice: when the request starts and when it ends, so a slow write stays attributable while
        // the disk events and the index rebuild it causes are still arriving.
        verify(changeOrigin, times(2)).remember(any());
    }

    @Test
    void marks_a_change_made_through_the_rest_api_or_the_legacy_pages_too() throws Exception {
        // Neither goes through the Spring dispatcher's interceptors, and both change the workspace
        // of a user whose browser tab must not read their changes as its own echo.
        handle("POST", "/rest/projects/p1/files/a.txt");
        handle("POST", "/faces/pages/modules/index.xhtml");

        verify(changeOrigin, times(4)).remember(any());
    }

    @Test
    void a_read_changes_nothing_and_marks_nobody() throws Exception {
        handle("GET", "/web/projects");

        verify(changeOrigin, never()).remember(any());
    }

    @Test
    void starts_before_there_is_an_application_context_to_ask() {
        // The container starts its filters after the context listeners, and the application configuration can
        // be reloading right then. A filter that asks for a bean to start fails the start-up of the whole web
        // application, and every page answers 503 until the server is restarted (EPBDS-16473).
        var startingContext = new MockServletContext();

        assertDoesNotThrow(() -> filter.init(new MockFilterConfig(startingContext, "changeOriginFilter")));
    }

    @Test
    void takes_the_resolver_of_the_configuration_in_force() throws Exception {
        handle("POST", "/web/projects/p1");
        var afterReload = publishResolver();

        handle("POST", "/web/projects/p1");

        // A configuration reload replaces the beans of the application context. A filter holding the resolver
        // it asked for once would go on naming the clients in an instance nothing reads any more.
        verify(changeOrigin, times(2)).remember(any());
        verifyNoMoreInteractions(changeOrigin);
        verify(afterReload, times(2)).remember(any());
    }

    /**
     * Puts a Spring context carrying a resolver of its own into the servlet context, the way the application
     * does when it starts and again after every configuration reload.
     *
     * <p>Each call publishes a context of its own, so the test says nothing about how a reload is carried out
     * and holds for a filter that keeps neither the resolver nor the context it came from.
     */
    private ChangeOriginResolver publishResolver() {
        var resolver = mock(ChangeOriginResolver.class);
        var applicationContext = mock(XmlWebApplicationContext.class);
        when(applicationContext.getBean(ChangeOriginResolver.class)).thenReturn(resolver);
        var initializer = new SpringInitializer();
        ReflectionTestUtils.setField(initializer, "applicationContext", applicationContext);
        servletContext.setAttribute(SpringInitializer.class.getName(), initializer);
        return resolver;
    }
}
