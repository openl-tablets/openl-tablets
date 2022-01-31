package org.openl.itest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.SocketUtils;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class UsersRestTest {

    private static final String INSERT_EXT_GROUPS_SQL = "INSERT INTO OpenL_External_Groups (loginName, groupName) VALUES ('%s', '%s');";
    private static final String TOKEN_PARAM = "token=";
    private static final int TOKEN_LENGTH = 8;

    private static JettyServer server;
    private static HttpClient client;
    private static GreenMail smtpServer;
    private static int smtpPort;

    private static Server h2Server;
    private static Connection h2Connection;
    private static final String DB_DUMP_FILE = String.format("target/dump-%s.sql", System.currentTimeMillis());

    @BeforeClass
    public static void setUp() throws Exception {
        h2Server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", "9111");
        h2Server.start();

        server = JettyServer.startWithWebXml("usr");
        client = server.client();

        smtpPort = SocketUtils.findAvailableTcpPort(1000);
        smtpServer = new GreenMail(new ServerSetup(smtpPort, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        smtpServer.setUser("username@email", "password");
        smtpServer.start();

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
        smtpServer.stop();
    }

    @After
    public void afterEach() throws SQLException, IOException, FolderException {
        try (Statement statement = h2Connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS DELETE FILES;");
            h2Connection.commit();

            String dump = new String(Files.readAllBytes(Paths.get(DB_DUMP_FILE)), Charset.defaultCharset());
            statement.execute(dump);
            h2Connection.commit();
        }
        smtpServer.purgeEmailFromAllMailboxes();
    }

    @Test
    public void smoke() {
        client.send("users-service/users-1.get");
        client.send("users-service/users-create.put");
        client.send("users-service/users-2.get");
        client.send("users-service/users-update.put");
        client.send("users-service/users-3.get");
        client.send("users-service/users-user.get");

        client.send("users-service/users-profile-1.get");
        client.send("users-service/users-profile-update.put");
        client.send("users-service/users-profile-2.get");

        client.send("users-service/users-delete-1.delete");
        client.send("users-service/users-delete-2.delete");
        client.send("users-service/users-1.get");
        client.send("users-service/users-info-update.put");
        client.send("users-service/users-4.get");

        client.send("users-service/users-options.get");
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

    @Test
    public void testMail() throws IOException, MessagingException {
        client.send("users-service/mail/users-mail-config-1.get");
        client.send("users-service/mail/users-mail-config-update-2.put");
        client.send("users-service/mail/users-mail-config-1.get");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic YWRtaW46YWRtaW4=");

        MailConfig newMailConfig = new MailConfig();
        newMailConfig.password = "password";
        newMailConfig.url = "smtp://127.0.0.1:" + smtpPort;
        newMailConfig.username = "username@email";
        client.putForObject("/web/mail/settings", newMailConfig, MailConfig.class, HttpStatus.NO_CONTENT, headers);

        MailConfig mailConfig = client.getForObject("/web/mail/settings", MailConfig.class, HttpStatus.OK, headers);
        Assert.assertEquals("password", mailConfig.password);
        Assert.assertEquals("username@email", mailConfig.username);
        Assert.assertEquals("smtp://127.0.0.1:" + smtpPort, mailConfig.url);

        int receivedMessagesCounter = 0;
        client.send("users-service/users-1.get");
        Assert.assertEquals(smtpServer.getReceivedMessages().length, receivedMessagesCounter);

        client.send("users-service/users-create.put");
        client.send("users-service/users-2.get");
        Assert.assertEquals(smtpServer.getReceivedMessages().length, ++receivedMessagesCounter);

        client.send("users-service/users-update.put");
        client.send("users-service/users-3.get");
        Assert.assertEquals(smtpServer.getReceivedMessages().length, ++receivedMessagesCounter);

        client.send("users-service/users-profile-1.get");

        client.send("users-service/users-profile-update.put");
        client.send("users-service/users-profile-2.get");
        Assert.assertEquals(smtpServer.getReceivedMessages().length, ++receivedMessagesCounter);

        client.send("users-service/users-delete-1.delete");
        client.send("users-service/users-info-update.put");
        client.send("users-service/users-4.get");
        Assert.assertEquals(smtpServer.getReceivedMessages().length, ++receivedMessagesCounter);

        client.send("users-service/mail/users-mail-send.post");
        Assert.assertEquals(smtpServer.getReceivedMessages().length, ++receivedMessagesCounter);

        MimeMessage message = smtpServer.getReceivedMessages()[smtpServer.getReceivedMessages().length - 1];
        Assert.assertEquals("admin@email", message.getRecipients(Message.RecipientType.TO)[0].toString());
        Assert.assertEquals("username@email", message.getFrom()[0].toString());

        InputStreamReader inputStreamReader = new InputStreamReader(message.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String content = bufferedReader.lines().collect(Collectors.joining());
        int tokenStartIndex = content.indexOf(TOKEN_PARAM) + TOKEN_PARAM.length();
        String token = content.substring(tokenStartIndex, tokenStartIndex + TOKEN_LENGTH);
        client.getForObject("/web/mail/verify/" + token, String.class, HttpStatus.NO_CONTENT);
        inputStreamReader.close();
        bufferedReader.close();

        client.send("users-service/mail/users-mail-verified.get");

        client.send("users-service/mail/users-mail-config-update-2.put");
        client.send("users-service/mail/users-mail-config-1.get");
    }

    public static class MailConfig {
        public String url;
        public String username;
        public String password;
    }
}
