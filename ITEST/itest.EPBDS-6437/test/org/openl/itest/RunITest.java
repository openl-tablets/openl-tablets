package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.generated.beans.Vehicle;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.SoapClientFactory;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.itest.rules.Service;
import org.openl.rules.variation.JXPathVariation;
import org.openl.rules.variation.VariationException;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class RunITest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
    private static final HttpHeaders DEF_HTTPHEADERS;

    private static JettyServer server;
    private static HttpClient client;
    private static String baseURI;

    static {
        DEF_HTTPHEADERS = new HttpHeaders();
        DEF_HTTPHEADERS.setContentType(MediaType.APPLICATION_JSON);
        DEF_HTTPHEADERS.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    private RestTemplate rest;
    private Service soapClient;

    @BeforeClass
    public static void setUp() throws Exception {
        Locale.setDefault(Locale.US);

        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);

        server = new JettyServer(true);
        baseURI = server.start();
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

    private static Date toDate(int year, int month, int dayOfMonth, int hour, TimeZone tz) {
        Calendar calendar = Calendar.getInstance(tz);
        calendar.set(year, month - 1, dayOfMonth, hour, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Before
    public void before() {
        rest = new RestClientFactory(baseURI).setSupportVariations(true).create();

        soapClient = new SoapClientFactory<>(baseURI + "/EPBDS-6437", Service.class).setSupportVariations(true)
            .createProxy();
    }

    @Test
    public void testCalVehicleYear_StringRequest_JSON() {
        client.post("/REST/EPBDS-6437/calVehicleYear",
            "/testCalVehicleYear_StringRequest_JSON_request.json",
            "/testCalVehicleYear_StringRequest_JSON_response.txt");
    }

    @Test
    public void testCalVehicleYear_ObjectRequest_JSON() {
        client.post("/REST/EPBDS-6437/calVehicleYear",
            "/testCalVehicleYear_ObjectRequest_JSON_request.json",
            "/testCalVehicleYear_ObjectRequest_JSON_response.txt");

    }

    @Test
    public void testCheckRulesModule_usingLocaleTimezone_JSON() {
        client.post("/REST/EPBDS-6437/checkRulesModule",
            "/testCheckRulesModule_usingLocaleTimezone_JSON_request.json",
            "/testCheckRulesModule_usingLocaleTimezone_JSON_response.txt");
    }

    @Test
    public void testCheckRulesModule_usingUTCTimezone_JSON() {
        client.post("/REST/EPBDS-6437/checkRulesModule",
            "/testCheckRulesModule_usingUTCTimestamp_JSON_request.json",
            "/testCheckRulesModule_usingUTCTimestamp_JSON_response.txt");
    }

    @Test
    public void testCheckRulesModule_usingUTCTimestamp_JSON() {
        client.post("/REST/EPBDS-6437/checkRulesModule",
            "/testCheckRulesModule_usingUTCTimestamp_JSON_request.json",
            "/testCheckRulesModule_usingUTCTimestamp_JSON_response.txt");
    }

    @Test
    public void testCalVehicleYear_usingLocalTimezone_SOAP() {
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCurrentDate(Calendar.getInstance().getTime());

        Vehicle v = new Vehicle(2007, toDate(2016, 12, 31, 22, TimeZone.getDefault()));

        assertEquals(new Integer(9), soapClient.calVehicleYear(context, v));
    }

    @Test
    public void testCalVehicleYear_usingUTCTimezone_SOAP() {
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCurrentDate(Calendar.getInstance().getTime());

        Vehicle v = new Vehicle(2007, toDate(2016, 12, 31, 22, TimeZone.getTimeZone("UTC")));

        assertEquals(new Integer(10), soapClient.calVehicleYear(context, v));
    }

    @Test
    public void testCheckRulesModule_usingLocaleTimezone_SOAP() {
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCurrentDate(Calendar.getInstance().getTime());
        context.setRequestDate(toDate(2017, 12, 31, 22, TimeZone.getDefault()));

        assertEquals("20170101", soapClient.checkRulesModule(context));
    }

    @Test
    public void testCheckRulesModule_usingUTCTimezone_SOAP() {
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCurrentDate(Calendar.getInstance().getTime());
        context.setRequestDate(toDate(2017, 12, 31, 22, TimeZone.getTimeZone("UTC")));

        assertEquals("20180101", soapClient.checkRulesModule(context));
    }

    @Test
    public void testCalVehicleYearVariationsPack_SOAP() throws VariationException {
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCurrentDate(Calendar.getInstance().getTime());

        Vehicle v = new Vehicle(2007, toDate(2016, 12, 31, 22, TimeZone.getDefault()));
        VariationsPack variationPack = new VariationsPack();
        variationPack.addVariation(new JXPathVariation("variation_0",
            0,
            "/vehEffectiveYear",
            toDate(2016, 12, 31, 22, TimeZone.getDefault())));
        variationPack.addVariation(new JXPathVariation("variation_1",
            0,
            "/vehEffectiveYear",
            toDate(2016, 12, 31, 22, TimeZone.getTimeZone("UTC"))));

        VariationsResult<Integer> result = soapClient.calVehicleYear(context, v, variationPack);
        assertFalse(result.getVariationResults().isEmpty());
        assertEquals(new Integer(9), result.getVariationResults().get("variation_0"));
        assertEquals(new Integer(10), result.getVariationResults().get("variation_1"));
        assertEquals(new Integer(9), result.getVariationResults().get("Original calculation"));

        assertTrue(result.getVariationFailures().isEmpty());
    }

}
