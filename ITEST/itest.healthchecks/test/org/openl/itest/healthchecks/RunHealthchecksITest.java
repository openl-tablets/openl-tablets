package org.openl.itest.healthchecks;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class RunHealthchecksITest {

    @Test
    public void test() throws Exception {
        JettyServer.get().test();
    }

    @Test
    public void incorrectConfiguration() throws Exception {
        JettyServer.test("incorrect");
    }
}
