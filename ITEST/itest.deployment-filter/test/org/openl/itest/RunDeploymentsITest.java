package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunDeploymentsITest {

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
    public void server1PingIncludedProjects_OK() {
        client.send("pong-1.get");
        client.send("pong-3.get");
        client.send("pong-3.get");
    }

    @Test
    public void server1PingExcludedProject_shouldNotBeAccessible() {
        client.send("ping.get");
    }
}
