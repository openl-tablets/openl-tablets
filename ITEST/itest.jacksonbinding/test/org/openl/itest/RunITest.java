package org.openl.itest;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunITest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        Locale.setDefault(Locale.US);

        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);

        server = JettyServer.startSharingClassLoader();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stop();
        } finally {
            Locale.setDefault(DEFAULT_LOCALE);
            TimeZone.setDefault(DEFAULT_TIMEZONE);
        }
    }

    @Test
    public void testSerializationInclusionAlwaysConfiguration() {
        client.get("/rules-serializationInclusionAlways/getObject", "/serialization_inclusion_always.json");
        client.get("/rules-serializationInclusionAlways/swagger.json", "/serialization_inclusion_always_swagger.json");
        client.get("/rules-serializationInclusionAlways/openapi.json", "/serialization_inclusion_always_openapi.json");
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
        client.get("/rules-defaultdateformat/getDate", "/default_date_format.txt");
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
