package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class WebStudioTest {

    @Test
    void demo() throws Exception {
        JettyServer.get().test();
    }
}
