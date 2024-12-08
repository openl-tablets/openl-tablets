package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class RunWebservicesCSRFalseITest {

    @Test
    public void test() throws Exception {
        JettyServer.get().test();
    }
}
