package org.openl.itest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class UsersRestTest {

    private static final String INSERT_EXT_GROUPS_SQL = "INSERT INTO OpenL_External_Groups (loginName, groupName) VALUES ('%s', '%s');";

    private static JettyServer server;
    private static HttpClient client;

    private static Server h2Server;
    private static Connection h2Connection;
    private static final String DB_DUMP_FILE = String.format("target/dump-%s.sql", System.currentTimeMillis());

    @BeforeClass
    public static void setUp() throws Exception {
        h2Server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", "9111");
        h2Server.start();

        server = JettyServer.startWithWebXml("usr");
        client = server.client();

        h2Connection = DriverManager.getConnection("jdbc:h2:tcp://localhost:9111/mem:mydb");
        h2Connection.setAutoCommit(false);
        try (Statement statement = h2Connection.createStatement()) {
            statement.execute(String.format("SCRIPT TO '%s'", DB_DUMP_FILE));
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
        h2Connection.close();
        h2Server.stop();
    }

    @After
    public void afterEach() throws SQLException, IOException {
        try (Statement statement = h2Connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS DELETE FILES;");
            h2Connection.commit();

            String dump = new String(Files.readAllBytes(Paths.get(DB_DUMP_FILE)), Charset.defaultCharset());
            statement.execute(dump);
            h2Connection.commit();
        }
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

    @Test
    public void testExternalGroups() throws SQLException {
        client.send("admin/management/groups/groupsAddAdmin.json.post");
        client.send("users-service/users-create-1.put");
        client.send("users-service/users-5.get");
        client.send("users-service/users/groups/external/empty.jsmith.get");

        try (Statement statement = h2Connection.createStatement()) {
            statement.addBatch(String.format(INSERT_EXT_GROUPS_SQL, "jsmith", "GROUP_1"));
            statement.addBatch(String.format(INSERT_EXT_GROUPS_SQL, "jsmith", "GROUP_2"));
            statement.addBatch(String.format(INSERT_EXT_GROUPS_SQL, "jsmith", "GROUP_3"));
            statement.addBatch(String.format(INSERT_EXT_GROUPS_SQL, "jsmith", "Analysts"));
            statement.addBatch(String.format(INSERT_EXT_GROUPS_SQL, "jsmith", "Admiral Nelson"));

            statement.executeBatch();
            h2Connection.commit();
        }

        client.send("users-service/users/user-info.jsmith.get");
        client.send("users-service/users/groups/external/allExternal.jsmith.get");
        client.send("users-service/users/groups/external/matchedExternal.jsmith.get");
        client.send("users-service/users/groups/external/notMatchedExternal.jsmith.get");
        client.send("admin/management/groups/searchExternalGroup.json.get");
        client.send("users-service/users-delete-1.delete");
    }

}
