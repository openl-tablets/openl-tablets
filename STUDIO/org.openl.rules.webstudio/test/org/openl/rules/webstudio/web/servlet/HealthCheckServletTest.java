package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

class HealthCheckServletTest {

    private final HealthCheckServlet servlet = new HealthCheckServlet();
    private final MockServletContext servletContext = new MockServletContext();

    @Test
    void startup() throws Exception {
        assertResponse("/healthcheck/startup", 200, "text/plain;charset=UTF-8", "UP");
    }

    @Test
    void responseWriterFailureReturnsInternalServerError() throws Exception {
        var request = new MockHttpServletRequest(servletContext, "GET", "/healthcheck/startup");
        request.setServletPath("/healthcheck/startup");
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new IOException("Connection closed"));

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void readinessIsUnavailableBeforeSpringContextInitialization() throws Exception {
        assertResponse("/healthcheck/readiness", 503, null, "");
    }

    @Test
    void readiness() throws Exception {
        initializeSpringContext(true);

        assertResponse("/healthcheck/readiness", 200, "text/plain;charset=UTF-8", "READY");
    }

    @Test
    void readinessIsUnavailableAfterFailedSpringContextRefresh() throws Exception {
        initializeSpringContext(false);

        assertResponse("/healthcheck/readiness", 503, null, "");
    }

    @Test
    void readinessIsUnavailableWhileSpringContextIsRefreshing() throws Exception {
        var initializer = initializeSpringContext(true);
        var writeLock = (Lock) ReflectionTestUtils.getField(initializer, "write");
        assertNotNull(writeLock);
        var lockAcquired = new CountDownLatch(1);
        var releaseLock = new CountDownLatch(1);
        var refreshThread = Thread.ofVirtual().start(() -> holdLock(writeLock, lockAcquired, releaseLock));

        try {
            assertTrue(lockAcquired.await(1, TimeUnit.SECONDS));
            assertResponse("/healthcheck/readiness", 503, null, "");
        } finally {
            releaseLock.countDown();
            refreshThread.join();
        }
    }

    @Test
    void readinessIsUnavailableDuringShutdown() throws Exception {
        var initializer = initializeSpringContext(true);
        try (var ignored = mockStatic(Git.class)) {
            initializer.contextDestroyed(new ServletContextEvent(servletContext));
        }
        servletContext.setAttribute(SpringInitializer.class.getName(), initializer);

        assertResponse("/healthcheck/readiness", 503, null, "");
    }

    private SpringInitializer initializeSpringContext(boolean active) {
        var applicationContext = mock(XmlWebApplicationContext.class);
        when(applicationContext.isActive()).thenReturn(active);
        var initializer = new SpringInitializer();
        ReflectionTestUtils.setField(initializer, "applicationContext", applicationContext);
        servletContext.setAttribute(SpringInitializer.class.getName(), initializer);
        return initializer;
    }

    private static void holdLock(Lock lock, CountDownLatch lockAcquired, CountDownLatch releaseLock) {
        lock.lock();
        try {
            lockAcquired.countDown();
            releaseLock.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void assertResponse(String path, int expectedStatus, String expectedContentType, String expectedBody)
            throws Exception {
        var request = new MockHttpServletRequest(servletContext, "GET", path);
        request.setServletPath(path);
        var response = new MockHttpServletResponse();

        servlet.service(request, response);

        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedContentType, response.getContentType());
        assertEquals(expectedBody, response.getContentAsString());
    }
}
