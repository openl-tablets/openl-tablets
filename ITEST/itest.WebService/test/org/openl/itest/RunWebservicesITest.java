package org.openl.itest;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunWebservicesITest {

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
    public void testWadlSchemaSimple1() {
        client.get("/REST/deployment1/simple1?_wadl", "/simple1_wadl.resp.xml");
    }

    @Test
    public void testWsdlSchemaSimple1() {
        client.get("/deployment1/simple1?wsdl", "/simple1_wsdl.resp.xml");
    }

    @Test
    public void testSimple1_invoke_with_Variations() {
        client
            .post("/REST/deployment1/simple1/test1", "/simple1_invoke_test.req.json", "/simple1_invoke_test.resp.json");
    }

    @Test
    public void testWadlSchemaSimple2() {
        client.get("/REST/deployment2/simple2?_wadl", "/simple2_wadl.resp.xml");
    }

    @Test
    public void testWsdlSchemaSimple2() {
        client.get("/deployment2/simple2?wsdl", "/simple2_wsdl.resp.xml");
    }

    @Test
    public void testSwaggerSchemaSimple3() {
        client.get("/REST/deployment3/simple3/swagger.json", "/simple3_swagger.resp.json");
    }

    @Test
    public void testWADLSchemaSimple3() {
        client.get("/REST/deployment3/simple3?_wadl", "/simple3_wadl.resp.xml");
    }

    @Test
    @Ignore
    // Test is correct but result comparision doesn't work.
    public void testWSDLSchemaSimple3() {
        client.get("/deployment3/simple3?wsdl", "/simple3_wsdl.resp.xml");
    }

    @Test
    public void testSimple3_CSPR_Convert() {
        client.post("/REST/deployment3/simple3/main", "/simple3_main.req.json", "/simple3_main.resp.json");
        client.post("/REST/deployment3/simple3/mySpr", "/simple3_mySpr_a.req.json", "/simple3_mySpr_a.resp.json");
        client.post("/REST/deployment3/simple3/mySpr", "/simple3_mySpr_b.req.json", "/simple3_mySpr_b.resp.json");
        client.post("/REST/deployment3/simple3/mySpr2", "/simple3_mySpr2_a.req.json", "/simple3_mySpr2_a.resp.json");
        client.post("/REST/deployment3/simple3/mySpr2", "/simple3_mySpr2_b.req.json", "/simple3_mySpr2_b.resp.json");
        client.post("/REST/deployment3/simple3/arrSpreadsheetResult",
            "/simple3_main.req.json",
            "/simple3_arrSpreadsheetResult.resp.json");
        client.post("/REST/deployment3/simple3/arrSpreadsheetResultspr1",
            "/simple3_main.req.json",
            "/simple3_arrSpreadsheetResultspr1.resp.json");
    }

    @Test
    public void testSimple4SerializationInclusion() {
        client.post("/REST/deployment4/simple4/main",
            "/simple3_main.req.json",
            "/simple4_main_response_serialization_inclusion.json");
    }

    @Test
    public void testSimple3_CSPR_Convert_2() {
        client.post("/deployment3/simple3", "/simple3_main.req.xml", "/simple3_main.resp.xml");
    }

    @Test
    public void testWadlSchemaSimple5() {
        client.get("/REST/deployment5/simple5?_wadl", "/simple5_wadl.resp.xml");
    }

    @Test
    public void testSwaggerSchemaSimple5() {
        client.get("/REST/deployment5/simple5/swagger.json", "/simple5_swagger.resp.json");
    }

    @Test
    public void EPBDS_9500() {
        client.post("/REST/EPBDS-9500/EPBDS-9500/myRules",
            "/EPBDS-9500/EPBDS-9500_myRules.req.txt",
            "/EPBDS-9500/EPBDS-9500_myRules.resp.txt");
    }

    @Test
    public void EPBDS_9519() throws InterruptedException {
        final int MAX_THREADS = 1;
        Thread[] threads = new Thread[MAX_THREADS];
        AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i] = new Thread(() -> {
                client.get("/EPBDS-9519/EPBDS-9519/swagger.json", "/EPBDS-9519/EPBDS-9519_swagger.resp.json");
                counter.getAndIncrement();
            });
            threads[i].start();
        }
        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i].join();
        }
        assertEquals(MAX_THREADS, counter.get());
    }

    @Test
    public void EPBDS_9519_2() {
        client.get("/REST/EPBDS-9519_2/EPBDS-9519/swagger.json", "/EPBDS-9519/EPBDS-9519_2_swagger.resp.json");
    }

    @Test
    public void EPBDS_9519_3() {
        client.get("/REST/EPBDS-9519_3/EPBDS-9519/swagger.json", "/EPBDS-9519/EPBDS-9519_3_swagger.resp.json");
    }

    @Test
    public void EPBDS_9572() {
        client.get("/REST/EPBDS-9572/EPBDS-9572/swagger.json", "/EPBDS_9572/EPBDS_9572_swagger.resp.json");
    }

    @Test
    public void EPBDS_9581() {
        client.get("/EPBDS-9581/EPBDS-9581/swagger.json", "/EPBDS-9581/EPBDS-9581_swagger.resp.json");
        client.get("/EPBDS-9581/EPBDS-9581?_wadl", 404);
    }

}
