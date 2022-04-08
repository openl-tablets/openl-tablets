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
        server = JettyServer.startWithWebXml("wcs");
        client = server.client();
    }

    @Test
    public void test() {
        client.test("test-resources/EPBDS-12593_WebResource");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

}
