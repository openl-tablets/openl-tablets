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
        server = new JettyServer();
        String baseURI = server.start();
        client = HttpClient.create(baseURI);
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
    public void testWadlSchemaSimple2() {
        client.get("/REST/deployment2/simple2?_wadl", "/simple2_wadl.resp.xml");
    }

    @Test
    public void testWsdlSchemaSimple2() {
        client.get("/deployment2/simple2?wsdl", "/simple2_wsdl.resp.xml");
    }

    @Test
    public void testSimple3_CSPR_Convert() {
        client.post("/REST/deployment3/simple3/main", "/simple3_main.req.json", "/simple3_main.resp.json");
        client.post("/REST/deployment3/simple3/mySpr", "/simple3_mySpr_a.req.json", "/simple3_mySpr_a.resp.json");
        client.post("/REST/deployment3/simple3/mySpr", "/simple3_mySpr_b.req.json", "/simple3_mySpr_b.resp.json");
        client.post("/REST/deployment3/simple3/mySpr2", "/simple3_mySpr2_a.req.json", "/simple3_mySpr2_a.resp.json");
        client.post("/REST/deployment3/simple3/mySpr2", "/simple3_mySpr2_b.req.json", "/simple3_mySpr2_b.resp.json");
    }

    @Test
    public void testSimple3_CSPR_Convert_2() {
        client.post("/deployment3/simple3", "/simple3_main.req.xml", "/simple3_main.resp.xml");
    }
}
