package org.openl.itest;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

class RunLargePayloadTest {

    @AutoClose
    private static final HttpClient client = JettyServer.get().start();

    @RepeatedTest(200)
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    void detect_memory_leak_with_cxf_logger() {
        client.test("test-resources");
    }
}
