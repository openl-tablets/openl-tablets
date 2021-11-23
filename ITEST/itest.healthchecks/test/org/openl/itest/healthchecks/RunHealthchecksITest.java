package org.openl.itest.healthchecks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunHealthchecksITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.start();
        client = server.client();
    }

    @AfterClass
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

        client.post("/admin/deploy", "/rules-to-deploy.zip", 201);
        client.send("healthcheck/readiness_ready.get");

        client.post("/admin/deploy", "/rules-to-deploy-failed.zip", 201);
        client.send("healthcheck/readiness_failure.get");

        client.post("/admin/deploy", "/rules-to-deploy_v2.zip", 201);
        client.send("healthcheck/readiness_ready.get");
    }
}
