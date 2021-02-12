package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunWebservicesLazyITest {

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
    public void EPBDS_7757_testHttpStatuses() {
        client.get("/REST/http-statuses-test/throwUserException", 422, "/EPBDS-7757/rest_USER_ERROR_response.json");
        client.get("/REST/http-statuses-test/throwOpenLException", 500, "/EPBDS-7757/rest_RULES_RUNTIME_response.json");
        client.post("/REST/http-statuses-lazycompilation-test/throwCompilationError",
            "EPBDS-7757/rest_COMPULATION_request.json",
            500,
            "/EPBDS-7757/rest_COMPULATION_response.json");
        client.get("/REST/http-statuses-test/throwValidationException",
            422,
            "/EPBDS-7757/rest_RULES_RUNTIME_validation_exception_response.json");
        client.post("/REST/http-statuses-test/hello",
            "/EPBDS-7757/rest_JSON_EXCEPTION_request.json",
            400,
            "/EPBDS-7757/rest_JSON_EXCEPTION_response.json");
        client.post("/REST/http-statuses-test/hello",
            "/EPBDS-7757/statuses-415.resp.txt",
            415,
            "/EPBDS-7757/statuses-415.resp.txt");
        client.post("/REST/http-statuses-test/hello",
            "/EPBDS-7757/statuses-empty.req.json",
            200,
            "/EPBDS-7757/statuses-empty-request.resp.txt");
        client.post("/REST/http-statuses-test/throwNFE",
            "/EPBDS-7757/statuses-405.resp.txt",
            405,
            "/EPBDS-7757/statuses-405.resp.txt");
        client.get("/REST/http-statuses-test/throwNPE", 500, "/EPBDS-7757/rest_NPE_response.json");
        client.get("/REST/http-statuses-test/throwNFE", 500, "/EPBDS-7757/rest_NFE_response.json");
        client.get("/REST/http-statuses-test/hKllo", 404, "/EPBDS-7757/statuses-404.resp.txt");
        client.post("/http-statuses-test",
            "/EPBDS-7757/statuses-userError.req.xml",
            500,
            "/EPBDS-7757/statuses-userError.resp.xml");
        client.post("/http-statuses-test",
            "/EPBDS-7757/statuses-validation.req.xml",
            500,
            "/EPBDS-7757/statuses-validation.resp.xml");
        client.post("/http-statuses-test",
            "/EPBDS-7757/statuses-rulesRuntime.req.xml",
            500,
            "/EPBDS-7757/statuses-rulesRuntime.resp.xml");
        client.post("/http-statuses-test",
            "/EPBDS-7757/statuses-validation.req.xml",
            500,
            "/EPBDS-7757/statuses-validation.resp.xml");
        client.post("/http-statuses-lazycompilation-test",
            "/EPBDS-7757/statuses-lazycompilation.req.xml",
            500,
            "/EPBDS-7757/statuses-lazycompilation.resp.xml");
        client
            .post("/http-statuses-test", "/EPBDS-7757/statuses-npe.req.xml", 500, "/EPBDS-7757/statuses-npe.resp.xml");
        client
            .post("/http-statuses-test", "/EPBDS-7757/statuses-nfe.req.xml", 500, "/EPBDS-7757/statuses-nfe.resp.xml");
    }

    @Test
    public void EPBDS_9678() {
        client.post("/REST/EPBDS-9678/EPBDS-9678/someRule", "/EPBDS-9678/EPBDS-9678_someRule.req.json", 404);
        client.get("/admin/services/EPBDS-9678/EPBDS-9678/errors", "/EPBDS-9678/EPBDS-9678_compilation_errors.json");
        client.get("/admin/services/EPBDS-9678_MultiProject/EPBDS-9678-project1/errors",
            "/EPBDS-9678/multi/EPBDS-9678-project1_compilation_validation_errors.json");
        client.post("/EPBDS-9678-project2/someRule",
            "/EPBDS-9678/multi/EPBDS-9678-project2_someRule.req.json",
            "/EPBDS-9678/multi/EPBDS-9678-project2_someRule.resp.txt");
        client.post("/EPBDS-9678-project2/test1",
            "/EPBDS-9678/multi/EPBDS-9678-project2_someRule.req.json",
            "/EPBDS-9678/multi/EPBDS-9678-project2_someRule.resp.txt");
    }

    @Test
    public void EPBDS_10699() {
        client.get("/admin/services/EPBDS-10699/EPBDS-10699/errors", "/EPBDS-10699/EPBDS-10699_error.resp.json");
        client.get("/REST/EPBDS-10699/EPBDS-10699/openapi.json", "/EPBDS-10699/EPBDS-10699_openapi.resp.json");
        client.post("/REST/EPBDS-10699/EPBDS-10699/m",
            "/EPBDS-10699/EPBDS-10699_call.req.json",
            500,
            "/EPBDS-10699/EPBDS-10699_call.resp.json");
    }
}
