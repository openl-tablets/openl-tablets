package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class GroupsRestTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("groups");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void EPBDS_7698_user_groups_cyclic_dependencies() {
        client.send("EPBDS-7698_cyclic_groups/01-groupsAddGroupA");
        client.send("EPBDS-7698_cyclic_groups/02-groupsAddGroupBReferringToA");
        client.send("EPBDS-7698_cyclic_groups/03-groupsEditGroupAAddReferenceToB");

        client.send("EPBDS-7698_cyclic_groups/04-users-a-create-group-a");
        client.send("EPBDS-7698_cyclic_groups/05-users-a-get-profile");
    }


}
