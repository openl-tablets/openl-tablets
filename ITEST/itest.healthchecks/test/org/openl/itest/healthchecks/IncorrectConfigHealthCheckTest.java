package org.openl.itest.healthchecks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class IncorrectConfigHealthCheckTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.start("incorrect");
        client = server.client();
    }

    @AfterClass
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
