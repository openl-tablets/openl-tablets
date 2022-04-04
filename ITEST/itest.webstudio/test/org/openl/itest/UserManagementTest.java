package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class UserManagementTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("users");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void smoke() {
        client.send("admin/management/groups/groups.no-auth.json.get");
        client.send("admin/management/groups/groups.json.get");
        client.send("admin/management/groups/settings/getAll.json.get");

        client.send("admin/management/groups/settings/apply.1.json.post");
        client.send("admin/management/groups/settings/getAll.1.json.get");

        client.send("admin/management/groups/settings/apply.2.json.post");
        client.send("admin/management/groups/settings/getAll.2.json.get");

        client.send("admin/management/groups/settings/apply.invalid.1.json.post");
        client.send("admin/management/groups/settings/getAll.2.json.get"); // must be as before
    }

    @Test
    public void EPBDS_7698() {
        client.send("EPBDS-7698/add-group-1.json.post");
        client.send("EPBDS-7698/groups-1.json.get");
        client.send("EPBDS-7698/delete-group-1.json.post");
        client.send("EPBDS-7698/groups.json.get");
    }

}
