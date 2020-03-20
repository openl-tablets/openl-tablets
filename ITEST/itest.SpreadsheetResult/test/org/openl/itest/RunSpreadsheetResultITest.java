package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunSpreadsheetResultITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startSharingClassLoader();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void SpreadsheetResult_Wadl() {
        client.get("/REST/spreadsheetresult?_wadl", "/spreadsheetresult_wadl.resp.xml");
        client.get("/REST/EPBDS-9437?_wadl", "/EPBDS-9437_wadl.resp.xml");
    }

    @Test
    public void SpreadsheetResult_Swagger() {
        client.get("/REST/spreadsheetresult/swagger.json", "/spreadsheetresult_swagger.resp.json");
        client.get("/REST/EPBDS-9437/swagger.json", "/EPBDS-9437_swagger.resp.json");
    }

    @Test
    public void SpreadsheetResult_Wsdl() {
        client.get("/spreadsheetresult?wsdl", "/spreadsheetresult_wsdl.resp.xml");
        client.get("/EPBDS-9437?wsdl", "/EPBDS-9437_wsdl.resp.xml");
    }

    @Test
    public void SpreadsheetResult_REST() {
        client.post("/REST/spreadsheetresult/tiktak",
            "/spreadsheetresult_tiktak.req.json",
            "/spreadsheetresult_tiktak.resp.json");
        client.post("/REST/EPBDS-9437/tiktak", "/EPBDS-9437_tiktak.req.json", "/EPBDS-9437_tiktak.resp.json");
        client.post("/REST/EPBDS-9437/EPBDS_9437", "/EPBDS-9437_arr.req.txt", "/EPBDS-9437_arr.resp.json");
    }

    @Test
    public void SpreadsheetResult_SOAP() throws InterruptedException {
        client.post("/spreadsheetresult", "/spreadsheetresult_tiktak.req.xml", "/spreadsheetresult_tiktak.resp.xml");
        client.post("/EPBDS-9437", "/EPBDS-9437_tiktak.req.xml", "/EPBDS-9437_tiktak.resp.xml");
        client.post("/EPBDS-9437", "/EPBDS-9437_arr.req.xml", "/EPBDS-9437_arr.resp.xml");
    }
}
