package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class WebStudioTest {

    @Test
    void dtr() throws Exception {
        JettyServer.get().test();
    }
}
