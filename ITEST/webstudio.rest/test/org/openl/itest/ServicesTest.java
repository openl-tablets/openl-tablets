package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class ServicesTest {
    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void getTypesOnEmptyServer() {
        client.send("admin/tag-config/types.empty.json.get");
    }

    @Test
    public void smokeTest() {
        client.send("admin/tag-config/create-type.patch");
        client.send("admin/tag-config/smoke-types-1.get");

        client.send("admin/tag-config/create-tag-1.patch");
        client.send("admin/tag-config/smoke-types-2.get");

        client.send("admin/tag-config/create-tag-2.patch");
        client.send("admin/tag-config/smoke-types-3.get");

        client.send("admin/tag-config/edit-tag-2.patch");
        client.send("admin/tag-config/smoke-types-4.get");

        client.send("admin/tag-config/delete-tag");
        client.send("admin/tag-config/smoke-types-2.get");

        client.send("admin/tag-config/edit-type-1.patch");
        client.send("admin/tag-config/smoke-types-5.get");

        client.send("admin/tag-config/delete-type");
        client.send("admin/tag-config/types.empty.json.get");
    }
}
