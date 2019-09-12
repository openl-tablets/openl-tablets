package org.openl.itest;

import javax.xml.xpath.XPathExpressionException;

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
    public void testCases() {
        client.post("/upcs/lowCase", "/low-case.req.json", "/true-value.resp.txt");
        client.post("/upcs/UPCase", "/upper-case.req.json", "/true-value.resp.txt");
        client.post("/upcs/MixedCase", "/mixed-case.req.json", "/true-value.resp.txt");
        client.post("/upcs/eDGECase", "/edge-case.req.json", "/true-value.resp.txt");
    }

    @Test
    public void testOverload() {
        client.get("/upcs/overload", "/true-value.resp.txt");
        client.get("/upcs/overload2/1", "/true-value.resp.txt");
        client.post("/upcs/overload4", "/overload-4.req.json", "/true-value.resp.txt");
        client.post("/upcs/overload3", "/overload-3.req.json", "/true-value.resp.txt");
        client.post("/upcs/overload1", "/overload-1.req.json", "/true-value.resp.txt");
    }

    @Test
    public void testWadlSchemaForBean() throws XPathExpressionException {
        client.get("/upcs?_wadl", "/wadl.resp.xml");
    }

}
