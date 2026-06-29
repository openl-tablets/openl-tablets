package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class RunWebservicesITest {

    @Test
    void test() throws Exception {
        JettyServer.get().test();
    }

}
