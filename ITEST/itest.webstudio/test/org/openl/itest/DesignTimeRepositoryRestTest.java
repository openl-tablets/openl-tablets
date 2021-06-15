package org.openl.itest;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class DesignTimeRepositoryRestTest {

    private static String OPENL_HOME;

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        OPENL_HOME = System.getProperty("openl.home");
        System.setProperty("openl.home", Files.createTempDirectory(Paths.get("target").toAbsolutePath(), "webstudio-").toString());
        server = JettyServer.startWithWebXml();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
        System.setProperty("openl.home", OPENL_HOME);
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
        client.send("desing-time-repo/git-non-flat/update-proj-1.put");
        client.send("desing-time-repo/git-non-flat/update-proj-2.put");
        client.send("desing-time-repo/git-non-flat/update-proj-4.put");
        client.send("desing-time-repo/git-non-flat/update-proj-5.put");
        client.send("desing-time-repo/git-non-flat/repos-projects-2.get");
    }

}
