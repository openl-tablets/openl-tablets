package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class WebResourceTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("simple");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void test() {
        client.send("EPBDS-12593_WebResource/00-initialize.get");
        client.send("EPBDS-12593_WebResource/01-resource.get");
        client.send("EPBDS-12593_WebResource/02-resource.get");
        client.send("EPBDS-12593_WebResource/03-resource.get");
        client.send("EPBDS-12593_WebResource/04-resource.get");
        client.send("EPBDS-12593_WebResource/05-resource.get");
        client.send("EPBDS-12593_WebResource/06-resource.get");
        client.send("EPBDS-12593_WebResource/07-resource.get");
    }


}
