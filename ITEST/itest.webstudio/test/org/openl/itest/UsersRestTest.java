package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class UsersRestTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("usr");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void smoke() {
        client.send("users-service/users-1.get");
        client.send("users-service/users-create.put");
        client.send("users-service/users-2.get");
        client.send("users-service/users-update.put");
        client.send("users-service/users-3.get");
        client.send("users-service/users-user.get");
        client.send("users-service/users-delete-1.delete");
        client.send("users-service/users-delete-2.delete");
        client.send("users-service/users-1.get");
        client.send("users-service/users-info-update.put");
        client.send("users-service/users-4.get");

        client.send("users-service/users-options.get");

        client.send("users-service/users-profile-1.get");
        client.send("users-service/users-profile-update.put");
        client.send("users-service/users-profile-2.get");

    }

}
