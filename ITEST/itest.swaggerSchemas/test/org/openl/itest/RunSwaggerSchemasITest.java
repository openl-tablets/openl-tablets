package org.openl.itest;

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
        server = JettyServer.startSharingClassLoader();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testSwaggerSchemaWithRuntimeContext() {
        client.get("/rules-with-runtime-context/swagger.json", "/swagger-context.resp.json");
        client.get("/rules-with-runtime-context/openapi.json", "/openapi-context.resp.json");
    }

    @Test
    public void testSwaggerSchemaWithSpacesInUrl() throws Exception {
        client.get("/service name with spaces/swagger.json", "/swagger-spaces-in-url.resp.json");
        client.get("/service name with spaces/openapi.json", "/openapi-spaces-in-url.resp.json");
    }

    @Test
    public void testSwaggerSchemaWithoutRuntimeContext() {
        client.get("/rules-without-runtime-context/swagger.json", "/swagger-no-context.resp.json");
        client.get("/rules-without-runtime-context/openapi.json", "/openapi-no-context.resp.json");
    }
}
