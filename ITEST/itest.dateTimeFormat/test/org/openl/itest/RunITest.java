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

        server = JettyServer.start();
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
    public void testCalVehicleYear_JSON() {
        client.post("/REST/EPBDS-6437/calVehicleYear", "/localTime-calc.req.json", "/localTime-calc.resp.txt");
        client.post("/REST/EPBDS-6437/calVehicleYear", "/UTCTimezone-calc.req.json", "/UTCTimezone-calc.resp.txt");
    }

    @Test
    public void testCheckRulesModule_JSON() {
        client.post("/REST/EPBDS-6437/checkRulesModule", "/localTime.req.json", "/localTime.resp.txt");
        client.post("/REST/EPBDS-6437/checkRulesModule", "/UTCTimestamp.req.json", "/UTCTimestamp.resp.txt");
        client.post("/REST/EPBDS-6437/checkRulesModule", "/UTCTimezone.req.json", "/UTCTimezone.resp.txt");
    }

    @Test
    public void testCalVehicleYear_SOAP() {
        client.post("/EPBDS-6437", "/localTime-calc.req.xml", "/localTime-calc.resp.xml");
        client.post("/EPBDS-6437", "/UTCTimezone-calc.req.xml", "/UTCTimezone-calc.resp.xml");
    }

    @Test
    public void testCheckRulesModule_SOAP() {
        client.post("/EPBDS-6437", "/localTime.req.xml", "/localTime.resp.xml");
        client.post("/EPBDS-6437", "/UTCTimezone.req.xml", "/UTCTimezone.resp.xml");
    }

    @Test
    public void testCalVehicleYearVariationsPack_SOAP() {
        client.post("/EPBDS-6437", "/variation.req.xml", "/variation.resp.xml");
    }

    @Test
    public void testFromDifferentDateFormats_JSON() {
        client.post("/REST/EPBDS-9201/spr", "/milliseconds.req.json", "/milliseconds.resp.json");
        client.post("/REST/EPBDS-9201/spr", "/defaultDateFormat.req.json", "/defaultDateFormat.resp.json");
        client.post("/REST/EPBDS-9201/spr", "/iso8601WithoutTime.req.json", "/iso8601WithoutTime.resp.json");
    }

    @Test
    public void testEPBDS_10642() {
        client.get("/EPBDS-10642/swagger.json", "/EPBDS-10642/EPBDS-10642/swagger.resp.json");
        client.get("/EPBDS-10642/openapi.json", "/EPBDS-10642/EPBDS-10642/openapi.resp.json");
        client.post("/EPBDS-10642/localDateProxy", "/EPBDS-10642/EPBDS-10642/localDateProxy.req.txt", "/EPBDS-10642/EPBDS-10642/localDateProxy.resp.txt");
        client.post("/EPBDS-10642/localTimeProxy", "/EPBDS-10642/EPBDS-10642/localTimeProxy.req.txt", "/EPBDS-10642/EPBDS-10642/localTimeProxy.resp.txt");
        client.post("/EPBDS-10642/localDateTimeProxy", "/EPBDS-10642/EPBDS-10642/localDateTimeProxy.req.txt", "/EPBDS-10642/EPBDS-10642/localDateTimeProxy.resp.txt");
        client.post("/EPBDS-10642/zonedDateTypeProxy", "/EPBDS-10642/EPBDS-10642/zonedDateTypeProxy.req.txt", "/EPBDS-10642/EPBDS-10642/zonedDateTypeProxy.resp.txt");
        client.post("/EPBDS-10642/instantProxy", "/EPBDS-10642/EPBDS-10642/instantProxy.req.txt", "/EPBDS-10642/EPBDS-10642/instantProxy.resp.txt");
        client.post("/EPBDS-10642/dateType", "/EPBDS-10642/EPBDS-10642/dateType.req.txt", "/EPBDS-10642/EPBDS-10642/dateType.resp.txt");
        client.post("/EPBDS-10642/spr", "/EPBDS-10642/EPBDS-10642/spr.req.json", "/EPBDS-10642/EPBDS-10642/spr.resp.json");
    }
}
