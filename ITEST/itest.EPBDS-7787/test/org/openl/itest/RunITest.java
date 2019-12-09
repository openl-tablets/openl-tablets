package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer();
        server.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testMethodFromFirstProject() {
        client.post("/EPBDS-7787-project1/calculation", "/calc.req.json", "/calc.resp.json");
    }

    @Test
    public void testMethodFromProject2_usingUsingInterfaceFromProject1() {
        client.get("/EPBDS-7787-project1/getProject2FirstPolicy", "/proj2.resp.json");
    }

    @Test
    public void testMethodFromProject1_usingUsingInterfaceFromProject1() {
        client.get("/EPBDS-7787-project1/getProject1FirstPolicy", "/proj1.resp.json");
    }
}
