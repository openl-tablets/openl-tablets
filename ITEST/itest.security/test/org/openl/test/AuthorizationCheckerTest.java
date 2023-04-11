package org.openl.test;

import org.junit.Test;
import org.openl.itest.core.JettyServer;

public class AuthorizationCheckerTest {

    @Test
    public void testSecurity() throws Exception {
        JettyServer.test("custom");
    }
}
