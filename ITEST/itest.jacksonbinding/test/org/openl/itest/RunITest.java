package org.openl.itest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer();
        baseURI = server.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
    }

    @Test
    public void testSerializationInclusionConfiguration() {
        client.get("/rules-serializationInclusion/getObject", "/serialization_inclusion.json");
    }

    @Test
    public void testDefaultDateFormatConfiguration() {

        client.get("/rules-defaultdateformat/getDate", "/default_dat_format.txt");
    }

    @Test
    public void testDisableDefaultTyping() {

        client.get("/rules-disabledefaulttyping/getObject", "/disable_default_typing.json");
    }

    @Test
    public void testSmartDefaultTyping() {

        client.get("/rules-smartdefaulttyping/myCat", "/smart_default_typing.json");
    }

}
