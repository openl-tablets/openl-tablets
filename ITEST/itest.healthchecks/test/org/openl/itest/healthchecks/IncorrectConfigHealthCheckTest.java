package org.openl.itest.healthchecks;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class IncorrectConfigHealthCheckTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeAll
    public static void setUp() throws Exception {
        server = JettyServer.start("incorrect");
        client = server.client();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testStartupOk() {
        client.send("incorrect-config/startup.get");
    }

    @Test
    public void testReadiness() {
        client.send("incorrect-config/readiness_failure.get");
    }
}
