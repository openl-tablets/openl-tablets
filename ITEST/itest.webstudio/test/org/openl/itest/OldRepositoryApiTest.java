package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class OldRepositoryApiTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("wcsadmin");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void EPBDS_5662() {
        client.send("users-service/users-info-update.put");
        client.send("EPBDS-5662/01-getAllProjects");
        client.send("EPBDS-5662/02-getLastProject");
        client.send("EPBDS-5662/03-addProject");
        client.send("EPBDS-5662/04-getProject");
        client.send("EPBDS-5662/05-getProject");
        client.send("EPBDS-5662/06-addProject");
        client.send("EPBDS-5662/07-addProject");
        client.send("EPBDS-5662/08-getProject");
        client.send("EPBDS-5662/09-getAllProjects");
    }

    @Test
    public void EPBDS_12683() {
        client.test("test-resources/EPBDS-12683");
    }

}
