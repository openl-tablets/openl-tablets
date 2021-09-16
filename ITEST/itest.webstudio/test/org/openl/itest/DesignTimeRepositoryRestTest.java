package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class DesignTimeRepositoryRestTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("dtr");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void smoke() {
        client.send("desing-time-repo/failed-auth.get");
        client.send("desing-time-repo/repos-list.get");
        client.send("desing-time-repo/repos-404-projects.get");

        client.send("desing-time-repo/h2/repos-projects.get");
        client.send("desing-time-repo/h2/create-proj-1.put");
        client.send("desing-time-repo/h2/create-proj-2.put");
        client.send("desing-time-repo/h2/create-proj-3.put");
        client.send("desing-time-repo/h2/update-proj-1.put");
        client.send("desing-time-repo/h2/update-proj-2.put");
        client.send("desing-time-repo/h2/repos-projects-2.get");

        client.send("users-service/users-info-update.put");

        client.send("desing-time-repo/git-flat/repos-projects.get");
        client.send("desing-time-repo/git-flat/create-proj-1.put");
        client.send("desing-time-repo/git-flat/create-proj-2.put");
        client.send("desing-time-repo/git-flat/create-proj-3.put");
        client.send("desing-time-repo/git-flat/update-proj-1.put");
        client.send("desing-time-repo/git-flat/update-proj-2.put");
        client.send("desing-time-repo/git-flat/repos-projects-2.get");

        client.send("desing-time-repo/git-non-flat/repos-projects.get");
        client.send("desing-time-repo/git-non-flat/create-proj-1.put");
        client.send("desing-time-repo/git-non-flat/create-proj-2.put");
        client.send("desing-time-repo/git-non-flat/create-proj-3.put");
        client.send("desing-time-repo/git-non-flat/create-proj-4.put");
        client.send("desing-time-repo/git-non-flat/create-proj-5.put");
        client.send("desing-time-repo/git-non-flat/create-proj-6.put");
        client.send("desing-time-repo/git-non-flat/create-proj-7.put");
        client.send("desing-time-repo/git-non-flat/update-proj-1.put");
        client.send("desing-time-repo/git-non-flat/update-proj-2.put");
        client.send("desing-time-repo/git-non-flat/update-proj-4.put");
        client.send("desing-time-repo/git-non-flat/update-proj-5.put");
        client.send("desing-time-repo/git-non-flat/update-proj-7.put");
        client.send("desing-time-repo/git-non-flat/update-proj-7-neg-1.put");
        client.send("desing-time-repo/git-non-flat/update-proj-7-neg-2.put");
        client.send("desing-time-repo/git-non-flat/repos-projects-2.get");

        client.send("desing-time-repo/git-protected-flat/repos-projects.get");
        client.send("desing-time-repo/git-protected-flat/create-proj-403.put");
    }

}
