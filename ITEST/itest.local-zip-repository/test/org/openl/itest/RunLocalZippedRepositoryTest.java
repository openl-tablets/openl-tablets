package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunLocalZippedRepositoryTest {

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
    public void testUiInfo() {
        client.send("admin_ui_info.json.get");
    }

    @Test
    public void testSwaggerSchema() {
        client.send("EPBDS_10917/swagger.json.get");
        client.send("EPBDS_10916/swagger.json.get");
    }

    @Test
    public void testOpenApiSchema() {
        client.send("EPBDS_10917/openapi.json.get");
        client.send("EPBDS_10916/openapi.json.get");
    }

    @Test
    public void testSingleProjectDeployment() {
        client.post("/REST/deployed-rules/hello", "/deployed-rules_hello.req.json", "/deployed-rules_hello.resp.txt");
        client.send("simple-jar/doSomething.json.post");
        client.send("rules-to-deploy/MANIFEST.MF.json.get");
    }

    @Test
    public void testMultiProjectDeployment() {
        client.post("/REST/project1/sayHello", "/project1_sayHello.req.txt", "/project1_sayHello.resp.txt");
        client.post("/yaml-project1/sayHello", "/project1_sayHello.req.txt", "/project1_sayHello.resp.txt");
        client.send("multiproject/multiproject.findCarByVIN.post");
    }

    @Test
    public void EPBDS_10917() {
        client.send("EPBDS_10917/Greeting.json.post");
    }

    @Test
    public void EPBDS_10916() {
        client.send("EPBDS_10916/Greeting.json.post");
    }

}
