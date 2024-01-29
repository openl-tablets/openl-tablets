package org.openl.test;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class AuthorizationCheckerTest {

    @Test
    public void testSecurity() throws Exception {
        JettyServer.test("custom");
    }
}
