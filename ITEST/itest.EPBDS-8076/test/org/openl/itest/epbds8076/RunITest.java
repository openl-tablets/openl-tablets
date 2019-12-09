package org.openl.itest.epbds8076;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RunITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer(true);
        server.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Parameters(name = "{index}: Test /{0} method. Expected result: {1}")
    public static Object[][] testDate() {
        return new Object[][] {
                {"/s1", "152.0"},
                {"/t1", "152.0"},
                {"/m1", "152.0"},
                {"/s2", "-38.0"},
                {"/t2", "-38.0"},
                {"/m2", "-38.0"}
        };
    }

    private final String methodName;
    private final String expectedResponse;

    public RunITest(String methodName, String expectedResponse) {
        this.methodName = methodName;
        this.expectedResponse = expectedResponse;
    }

    @Test
    public void testRest() {
        final String actual = client.post("/REST/EPBDS-8076/EPBDS-8076" + methodName, "/simple-request.json", String.class);
        assertEquals(expectedResponse, actual);
    }

    @Test
    public void testWadl() {
        client.post("/EPBDS-8076/EPBDS-8076", methodName + "-request.xml", methodName + "-response.xml");
    }

}
