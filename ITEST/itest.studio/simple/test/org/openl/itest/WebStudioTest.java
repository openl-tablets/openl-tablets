package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class WebStudioTest {

    @Test
    void simple() throws Exception {
        try (var client = JettyServer.get().start()) {
            client.test("test-resources-simple");
        }
    }
}
