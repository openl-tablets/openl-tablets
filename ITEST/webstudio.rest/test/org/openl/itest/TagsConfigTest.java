package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class TagsConfigTest {
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
    public void smokeTest() {
        client.send("admin/tag-config/types.empty.json.get");

        client.send("admin/tag-config/create-type.post");
        client.send("admin/tag-config/smoke-types-1.get");

        client.send("admin/tag-config/create-tag-1.post");
        client.send("admin/tag-config/smoke-types-2.get");

        client.send("admin/tag-config/create-tag-2.post");
        client.send("admin/tag-config/smoke-types-3.get");

        client.send("admin/tag-config/edit-tag-2.put");
        client.send("admin/tag-config/smoke-types-4.get");

        client.send("admin/tag-config/delete-tag");
        client.send("admin/tag-config/smoke-types-2.get");

        client.send("admin/tag-config/edit-type-1.put");
        client.send("admin/tag-config/smoke-types-5.get");

        client.send("admin/tag-config/delete-type");
        client.send("admin/tag-config/types.empty.json.get");

        client.send("admin/tag-config/delete-non-existent-tag");
        client.send("admin/tag-config/delete-non-existent-type");

        client.send("admin/tag-config/create-type.invalid");
        client.send("admin/tag-config/create-type.international");
        client.send("admin/tag-config/create-type.international.same");
        client.send("admin/tag-config/edit-type.invalid");

        client.send("admin/tag-config/create-tag-3.invalid");
        client.send("admin/tag-config/create-tag-4.international");
        client.send("admin/tag-config/create-tag-5.international.same");
        client.send("admin/tag-config/edit-tag-4.invalid");
        client.send("admin/tag-config/delete-tag-4.international");

        client.send("admin/tag-config/delete-type.international");
    }
}
