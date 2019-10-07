package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class JavaKeywordsTest {

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
    public void testRestGreeting() {
        client.post("/REST/EPBDS-6555/Greeting", "/Greeting_Request.json", "/Greeting_Response.json");
    }

    @Test
    public void testRestCalc() {
        client.post("/REST/EPBDS-6555/Calc", "/Calc_Request.json", "/Calc_Response.txt");
    }

    @Test
    public void testSoapGreeting() {
        client.post("/EPBDS-6555/Greeting", "/Greeting_Request.xml", "/Greeting_Response.xml");
    }

    @Test
    public void testSoapCalc() {
        client.post("/EPBDS-6555/Calc", "/Calc_Request.xml", "/Calc_Response.xml");
    }
}