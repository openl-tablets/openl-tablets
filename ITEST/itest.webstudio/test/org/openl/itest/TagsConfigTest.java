package org.openl.itest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class TagsConfigTest {
    private static String OPENL_CONFIG_LOCATION;
    private static String OPENL_HOME;

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        OPENL_CONFIG_LOCATION = System.getProperty("openl.config.location");
        System.setProperty("openl.config.location", "file:./openl-repository/single-user-application.properties");

        OPENL_HOME = System.getProperty("openl.home");
        System.setProperty("openl.home", Files.createTempDirectory(Paths.get("target").toAbsolutePath(), "tags-config-test-").toString());

        server = JettyServer.startWithWebXml();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();

        System.setProperty("openl.home", OPENL_HOME);
        System.setProperty("openl.config.location", OPENL_CONFIG_LOCATION);
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

        client.send("admin/tag-config/edit-absent-type");
        client.send("admin/tag-config/edit-absent-tag");
    }
}
