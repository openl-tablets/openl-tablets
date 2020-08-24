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
        client.get("/openl-project-1/ping", "/pong.resp.txt");
        client.get("/openl-project-2/ping", "/pong.resp.txt");
        client.get("/extra-project/ping", "/pong.resp.txt");
    }

    @Test
    public void server1PingExcludedProject_shouldNotBeAccessible() {
        client.get("/excluded-project/ping", 404, "/404.resp.html");
    }
}
