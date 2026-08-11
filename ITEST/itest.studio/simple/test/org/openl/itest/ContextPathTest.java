package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

/**
 * OpenL Studio is usually deployed under a context path rather than at the server root, and the context root is
 * then reachable both with and without the trailing slash. Without the slash the request carries no path info,
 * which the static resources servlet has to answer just like the one with it.
 */
class ContextPathTest {

    @Test
    void deployedUnderContextPath() throws Exception {
        try (var client = JettyServer.get().withContextPath("/openl-studio").start()) {
            client.test("test-resources-context-path");
        }
    }
}
