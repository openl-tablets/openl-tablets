package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class SmokeITest {

    @Test
    public void test() throws Exception {
        JettyServer.test();
    }
}
