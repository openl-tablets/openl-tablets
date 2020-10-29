package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunUnpackClasspathJarsTest {

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
    public void testSingleProjectDeploymentFromClasspath() {
        client.post("/REST/deployed-rules/hello", "/deployed-rules_hello.req.json", "/deployed-rules_hello.resp.txt");
    }

    @Test
    public void testMultiProjectDeploymentFromClasspath() {
        client.post("/REST/project1/sayHello", "/project1_sayHello.req.txt", "/project1_sayHello.resp.txt");
        client.post("/yaml-project1/sayHello", "/project1_sayHello.req.txt", "/project1_sayHello.resp.txt");
    }

}
