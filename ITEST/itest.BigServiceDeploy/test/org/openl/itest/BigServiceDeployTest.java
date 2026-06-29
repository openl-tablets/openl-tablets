package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class BigServiceDeployTest {

    @Test
    void test() throws Exception {
        JettyServer.get().test();
    }

}
