package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testSerializationInclusionAlwaysConfiguration() {
        client.get("/rules-serializationInclusionAlways/getObject", "/serialization_inclusion_always.json");
        client.get("/rules-serializationInclusionAlways/swagger.json", "/serialization_inclusion_always_swagger.json");
    }

    @Test
    public void testSerializationInclusionNonAbsentConfiguration() {
        client.get("/rules-serializationInclusionNonAbsent/getObject", "/serialization_inclusion_non_absent.json");
    }

    @Test
    public void testSerializationInclusionNonDefaultConfiguration() {
        client.get("/rules-serializationInclusionNonDefault/getObject", "/serialization_inclusion_non_default.json");
    }

    @Test
    public void testSerializationInclusionNonEmptyConfiguration() {
        client.get("/rules-serializationInclusionNonEmpty/getObject", "/serialization_inclusion_non_empty.json");
    }

    @Test
    public void testSerializationInclusionNonNullConfiguration() {
        client.get("/rules-serializationInclusionNonNull/getObject", "/serialization_inclusion_non_null.json");
    }

    @Test
    public void testDefaultDateFormatConfiguration() {
        client.get("/rules-defaultdateformat/getDate", "/default_dat_format.txt");
        client.post("/rules-defaultdateformat/spr", "/spr-dateFormat.req.json", "/spr-dateFormat.resp.json");
    }

    @Test
    public void testSmartDefaultTyping() {
        client.get("/rules-smartdefaulttyping/myCat", "/smart_default_typing.json");
    }

    @Test
    public void testDisableDefaultTyping() {
        client.get("/rules-disabledefaulttyping/getObject", "/disable_default_typing.json");
    }
}
