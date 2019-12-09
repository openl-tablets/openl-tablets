package org.openl.itest.epbds7654;

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
    public void testRestResponse() {
        client
            .post("/REST/EPBDS-7654/getDayDiff", "/dayDiff.req.txt", "/dayDiff.resp.txt");
    }

    @Test
    public void testSoapResponse() {
        client.post("/EPBDS-7654", "/dayDiff.req.xml", "/dayDiff.resp.xml");
        client.post("/EPBDS-7654", "/dayDiff-wrong.req.xml", 500, "/dayDiff-wrong.resp.xml");
    }
}
