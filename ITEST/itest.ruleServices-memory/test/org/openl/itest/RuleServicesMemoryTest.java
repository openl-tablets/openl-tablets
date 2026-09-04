package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

class RuleServicesMemoryTest {

    @AutoClose
    private static final HttpClient client = JettyServer.get().start();

    @Test
    void compilesProjectWithinHeapLimit() {
        assertEquals("READY", client.getForObject("/admin/healthcheck/readiness", String.class, 200));
    }
}
