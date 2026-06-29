package org.openl.test;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class AuthorizationCheckerTest {

    @Test
    void testSecurity() throws Exception {
        JettyServer.test("custom");
    }
}
