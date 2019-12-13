package org.openl.itest;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunSwaggerSchemasITest {

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
    public void testSwaggerSchemaWithRuntimeContext() throws IOException {
        client.get("/rules-with-runtime-context/swagger.json", "/swagger-context.resp.json");
    }

    @Test
    public void testSwaggerSchemaWithSpacesInUrl() {
        client.get("/service%20name%20with%20spaces/swagger.json", "/swagger-spaces-in-url.resp.json");
    }

    @Test
    public void testSwaggerSchemaWithoutRuntimeContext() throws IOException {
        client.get("/rules-without-runtime-context/swagger.json", "/swagger-no-context.resp.json");
    }
}
