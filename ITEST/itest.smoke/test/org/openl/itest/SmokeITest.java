package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class SmokeITest {

    @Test
    void test() throws Exception {
        JettyServer.get().test();
    }
}
