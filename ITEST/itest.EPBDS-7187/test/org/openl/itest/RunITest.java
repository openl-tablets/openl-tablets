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
    public void testMixes1() {
        client.post("/upcs/mixes", "/empty.req.json", "/mixes1-default.resp.json");
        client.post("/upcs/mixes", "/mixes1-filled-nulls.json", "/mixes1-filled-nulls.json");
        client.post("/upcs/mixes", "/mixes1-filled.json", "/mixes1-filled.json");
    }

    @Test
    public void testMixes2() {
        client.post("/upcs/mixes2", "/empty.req.json", "/mixes2-empty.resp.json");
        client.post("/upcs/mixes2", "/mixes2-default.req.json", "/mixes2-default.resp.json");
        client.post("/upcs/mixes2", "/mixes2-filled.req.json", "/mixes2-filled.resp.json");
    }

    @Test
    public void testSchemas() {
        client.get("/upcs?_wadl", "/wadl.resp.xml");
        client.get("/upcs/swagger.json", "/swagger.resp.json");

        // To reproduce an issue with classloader uncomment next line
        // io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource:49 set breakpoint here and go into the method
        // to see exception
        // client.get("/upcs/openapi.json", "/openapi.resp.json");
    }

}
