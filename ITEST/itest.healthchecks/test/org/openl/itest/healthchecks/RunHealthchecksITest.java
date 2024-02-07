package org.openl.itest.healthchecks;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunHealthchecksITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeAll
    public static void setUp() throws Exception {
        server = JettyServer.start();
        client = server.client();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testStartupOk() {
        client.send("healthcheck/startup.get");
    }

    @Test
    public void testReadiness() {
        client.send("healthcheck/readiness_empty.get");

        client.send("deploy_v1");
        client.send("healthcheck/readiness_ready.get");

        client.send("deploy_failed");
        client.send("healthcheck/readiness_failure.get");

        client.send("deploy_v2");
        client.send("healthcheck/readiness_ready.get");
    }
}
