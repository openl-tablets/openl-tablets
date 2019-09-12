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
        server = new JettyServer(true);
        String baseURI = server.start();
        client = HttpClient.create(baseURI);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void SpreadsheetResult_Wadl() {
        client.get("/REST/spreadsheetresult?_wadl", "/spreadsheetresult_wadl.resp.xml");
    }

    @Test
    public void SpreadsheetResult_Wsdl() {
        client.get("/spreadsheetresult?wsdl", "/spreadsheetresult_wsdl.resp.xml");
    }

    @Test
    public void SpreadsheetResult_REST() {
        client.post("/REST/spreadsheetresult/tiktak",
            "/spreadsheetresult_tiktak.req.json",
            "/spreadsheetresult_tiktak.resp.json");
    }

    @Test
    public void SpreadsheetResult_SOAP() {
        client.post("/spreadsheetresult",
            "/spreadsheetresult_tiktak.req.xml",
            "/spreadsheetresult_tiktak.resp.xml");
    }
}
