package org.openl.itest.healthchecks;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class RunHealthchecksITest {

    @Test
    void test() throws Exception {
        JettyServer.get().test();
    }

    @Test
    void incorrectConfiguration() throws Exception {
        JettyServer.test("incorrect");
    }
}
