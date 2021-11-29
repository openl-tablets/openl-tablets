package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class SmokeITest {

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
    public void testCors() {
        client.send("cors.enabled.options");
    }

    @Test
    public void testGZIP() {
        client.send("simple_mul.json.gzip.post");
        client.send("multiproject.findCarByVIN.gzip.post");
    }

    @Test
    public void testMultimodule() {
        client.send("multiproject.findCarByVIN.post");
    }

    @Test
    public void testSysInfo() {
        client.send("sys.json.get");
    }

    @Test
    public void testAdmin() {
        client.send("admin_services.get");
        client.send("admin_deploy_download.get");
        client.send("admin_deploy_delete.delete");
    }
}
