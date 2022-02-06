package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class SmokeTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("smoke");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void smoke() {
        client.send("public/info/sys.info.get");
        client.send("public/info/openl.json.get");
        client.send("public/notification.txt.get");

        client.send("admin/notification.txt-1.post");
        client.send("public/notification.txt-1.get");
        client.send("admin/notification.txt-1.delete");
        client.send("public/notification.txt.get");
    }

}
