package org.openl.itest.epbds8076;

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
        server = JettyServer.startSharingClassLoader();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testRest() {
        client.post("/REST/EPBDS-8076/EPBDS-8076/m1", "/empty.req.json", "/const1.resp.json");
        client.post("/REST/EPBDS-8076/EPBDS-8076/m2", "/empty.req.json", "/const2.resp.json");
        client.post("/REST/EPBDS-8076/EPBDS-8076/s1", "/empty.req.json", "/const1.resp.json");
        client.post("/REST/EPBDS-8076/EPBDS-8076/s2", "/empty.req.json", "/const2.resp.json");
        client.post("/REST/EPBDS-8076/EPBDS-8076/t1", "/empty.req.json", "/const1.resp.json");
        client.post("/REST/EPBDS-8076/EPBDS-8076/t2", "/empty.req.json", "/const2.resp.json");
    }

    @Test
    public void testWadl() {
        client.post("/EPBDS-8076/EPBDS-8076", "/m1.req.xml", "/m1.resp.xml");
        client.post("/EPBDS-8076/EPBDS-8076", "/m2.req.xml", "/m2.resp.xml");
        client.post("/EPBDS-8076/EPBDS-8076", "/s1.req.xml", "/s1.resp.xml");
        client.post("/EPBDS-8076/EPBDS-8076", "/s2.req.xml", "/s2.resp.xml");
        client.post("/EPBDS-8076/EPBDS-8076", "/t1.req.xml", "/t1.resp.xml");
        client.post("/EPBDS-8076/EPBDS-8076", "/t2.req.xml", "/t2.resp.xml");
    }

}
