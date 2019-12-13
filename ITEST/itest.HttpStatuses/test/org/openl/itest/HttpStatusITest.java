package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class HttpStatusITest {
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
    public void test_rest_USER_ERROR() {
        client.get("/REST/http-statuses-test/throwUserException", 422, "/rest_USER_ERROR_response.json");
    }

    @Test
    public void test_rest_RULES_RUNTIME() {
        client.get("/REST/http-statuses-test/throwOpenLException", 500, "/rest_RULES_RUNTIME_response.json");
    }

    @Test
    public void test_rest_COMPULATION() {
        client.post("/REST/http-statuses-lazycompilation-test/throwCompilationError",
            "rest_COMPULATION_request.json",
            500,
            "/rest_COMPULATION_response.json");
    }

    @Test
    public void test_rest_RULES_RUNTIME_validation_exception() {
        client.get("/REST/http-statuses-test/throwValidationException",
            422,
            "/rest_RULES_RUNTIME_validation_exception_response.json");
    }

    @Test
    public void test_rest_JSON_EXCEPTION() {
        client.post("/REST/http-statuses-test/hello",
            "/rest_JSON_EXCEPTION_request.json",
            400,
            "/rest_JSON_EXCEPTION_response.json");
    }

    @Test
    public void test_rest_UNSUPPORTED_MEDIA_TYPE() {
        client.post("/REST/http-statuses-test/hello", "/statuses-415.resp.txt", 415, "/statuses-415.resp.txt");
    }

    @Test
    public void test_rest_EMPTY_REQUEST() {
        client.post("/REST/http-statuses-test/hello",
            "/statuses-empty.req.json",
            200,
            "/statuses-empty-request.resp.txt");
    }

    @Test
    public void test_rest_NOT_ALLOWED() {
        client.post("/REST/http-statuses-test/throwNFE", "/statuses-405.resp.txt", 405, "/statuses-405.resp.txt");
    }

    @Test
    public void test_rest_NPE() {
        client.get("/REST/http-statuses-test/throwNPE", 500, "/rest_NPE_response.json");
    }

    @Test
    public void test_rest_NFE() {
        client.get("/REST/http-statuses-test/throwNFE", 500, "/rest_NFE_response.json");
    }

    @Test
    public void test_rest_NOT_FOUND() {
        client.get("/REST/http-statuses-test/hKllo", 404, "/statuses-404.resp.txt");
    }

    @Test
    public void test_soap_USER_ERROR() {
        client.post("/http-statuses-test", "/statuses-userError.req.xml", 500, "/statuses-userError.resp.xml");
    }

    @Test
    public void test_soap_RULES_RUNTIME() {
        client.post("/http-statuses-test", "/statuses-rulesRuntime.req.xml", 500, "/statuses-rulesRuntime.resp.xml");
    }

    @Test
    public void test_soap_RULES_RUNTIME_validation_exception() {
        client.post("/http-statuses-test", "/statuses-validation.req.xml", 500, "/statuses-validation.resp.xml");
    }

    @Test
    public void test_soap_COMPULATION() {
        client.post("/http-statuses-lazycompilation-test",
            "/statuses-lazycompilation.req.xml",
            500,
            "/statuses-lazycompilation.resp.xml");
    }

    @Test
    public void test_soap_NPE() {
        client.post("/http-statuses-test", "/statuses-npe.req.xml", 500, "/statuses-npe.resp.xml");
    }

    @Test
    public void test_soap_NFE() {
        client.post("/http-statuses-test", "/statuses-nfe.req.xml", 500, "/statuses-nfe.resp.xml");
    }
}
