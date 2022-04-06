package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class WizardTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("wizard");
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
    }

    @Test
    public void EPBDS_11649_openapi() {
        client.send("EPBDS-11649_OpenAPI_Wizzard/01-openapi.json");
    }

}
