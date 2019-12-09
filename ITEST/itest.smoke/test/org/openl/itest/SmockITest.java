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
    public void testPingRest() {
        client.get("/REST/simple/ping", "/simple_ping.resp.txt");
    }

    @Test
    public void testTwiceRest() {
        client.post("/REST/simple/twice", "/simple_twice.req.txt", "/simple_twice.resp.txt");
    }

    @Test
    public void testMulRest() {
        client.post("/REST/simple/mul", "/simple_mul.req.json", "/simple_mul.resp.txt");
    }

    @Test
    public void test404Rest() {
        client.post("/REST/simple/absent", "/simple_mul.req.json", 404, "/404.txt");
    }

    @Test
    public void testPingSoap() {
        client.post("/simple", "/simple_ping.req.xml", "/simple_ping.resp.xml");
    }

    @Test
    public void testTwiceSoap() {
        client.post("/simple", "/simple_twice.req.xml", "/simple_twice.resp.xml");
    }

    @Test
    public void testMulSoap() {
        client.post("/simple", "/simple_mul.req.xml", "/simple_mul.resp.xml");
    }

    @Test
    public void test404Soap() {
        client.post("/simple", "/simple_absent.req.xml", 500, "/simple_absent.resp.xml");
    }
}
