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

    @Test
    public void EPBDS_11649_openapi() {
        client.send("EPBDS-11649_OpenAPI_NoAuth/01-openapi.json");
    }

    @Test
    public void EPBDS_12721_buildInfo() {
        client.send("EPBDS-12721/admin_buildInfo.get");
    }

}
