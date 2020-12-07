package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class SmockITest {

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
    public void testIndex() {
        client.send("index.get");
    }

    @Test
    public void testPingRest() {
        client.send("simple_ping.get");
    }

    @Test
    public void testTwiceRest() {
        client.send("simple_twice.txt.post");
    }

    @Test
    public void testMulRest() {
        client.send("simple_mul.json.post");
    }

    @Test
    public void test404Rest() {
        client.send("simple_absent.json.post");
    }

    @Test
    public void testPingSoap() {
        client.send("simple_ping.post");
    }

    @Test
    public void testTwiceSoap() {
        client.send("simple_twice.xml.post");
    }

    @Test
    public void testMulSoap() {
        client.send("simple_mul.xml.post");
    }

    @Test
    public void test404Soap() {
        client.send("simple_absent.xml.post");
    }

    @Test
    public void testCors() {
        client.send("cors.enabled.options");
    }

    @Test
    public void testMultimodule() {
        client.send("multiproject.findCarByVIN.post");
    }
}
