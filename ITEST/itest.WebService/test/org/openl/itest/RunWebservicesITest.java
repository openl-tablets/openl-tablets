package org.openl.itest;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
    public void testSimple1_invoke_with_Variations() {
        client
            .post("/REST/deployment1/simple1/test1", "/simple1_invoke_test.req.json", "/simple1_invoke_test.resp.json");
    }

    @Test
    public void testSwaggerSchemaSimple3() {
        client.get("/REST/deployment3/simple3/swagger.json", "/simple3_swagger.resp.json");
        client.get("/REST/deployment3/simple3/openapi.json", "/simple3_openapi.resp.json");
    }

    @Test
    public void EPBDS_10026() {
        client.send("EPBDS-10026/EPBDS-10026_swagger.get");
        client.send("EPBDS-10026/EPBDS-10026_openapi.get");
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
        client.post("/REST/deployment3/simple3/arrObjSpreadsheetResult",
            "/simple3_main.req.json",
            "/simple3_arrObjSpreadsheetResult.resp.json");
    }

    @Test
    public void testSimple4SerializationInclusion() {
        client.post("/deployment4/simple4/main",
            "/simple3_main.req.json",
            "/simple4_main_response_serialization_inclusion.json");
    }

    @Test
    public void testSwaggerSchemaSimple5() {
        client.get("/deployment5/simple5/swagger.json", "/simple5_swagger.resp.json");
        client.get("/deployment5/simple5/openapi.json", "/simple5_openapi.resp.json");
    }

    @Test
    public void EPBDS_9422() {
        client.get("/REST/EPBDS-9422/EPBDS-9422/swagger.json", "/EPBDS-9422/EPBDS-9422_swagger.resp.json");
        client.get("/REST/EPBDS-9422/EPBDS-9422/openapi.json", "/EPBDS-9422/EPBDS-9422_openapi.resp.json");
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
        client.get("/EPBDS-9519_2/EPBDS-9519/swagger.json", "/EPBDS-9519/EPBDS-9519_2_swagger.resp.json");
        client.get("/EPBDS-9519_2/EPBDS-9519/openapi.json", "/EPBDS-9519/EPBDS-9519_2_openapi.resp.json");
    }

    @Test
    public void EPBDS_9519_3() {
        client.get("/EPBDS-9519_3/EPBDS-9519/swagger.json", "/EPBDS-9519/EPBDS-9519_3_swagger.resp.json");
        client.get("/EPBDS-9519_3/EPBDS-9519/openapi.json", "/EPBDS-9519/EPBDS-9519_3_openapi.resp.json");
    }

    @Test
    public void EPBDS_9572() {
        client.get("/EPBDS-9572/EPBDS-9572/swagger.json", "/EPBDS-9572/EPBDS-9572_swagger.resp.json");
        client.get("/EPBDS-9572/EPBDS-9572/openapi.json", "/EPBDS-9572/EPBDS-9572_openapi.resp.json");
    }

    @Test
    public void EPBDS_9581() {
        client.send("EPBDS-9581/EPBDS-9581_swagger.get");
        client.send("EPBDS-9581/EPBDS-9581_openapi.get");
    }

    @Test
    public void EPBDS_9453() {
        client.post("/EPBDS-9453/EPBDS-9453/proxyCustomer",
            "/EPBDS-9453/EPBDS-9453_proxyCustomer.req.json",
            "/EPBDS-9453/EPBDS-9453_proxyCustomer.resp.json");

    }

    @Test
    public void EPBDS_9619() {
        client.get("/EPBDS-9619/EPBDS-9619/swagger.json", "/EPBDS-9619/EPBDS-9619_swagger.resp.json");
        client.get("/EPBDS-9619/EPBDS-9619/openapi.json", "/EPBDS-9619/EPBDS-9619_openapi.resp.json");
    }

    @Test
    public void EPBDS_9622() {
        client.get("/REST/v1/EPBDS-9622/doSomething", "/EPBDS-9622/EPBDS-9622_v1.resp.txt");
        client.get("/REST/v2/EPBDS-9622/doSomething", "/EPBDS-9622/EPBDS-9622_v2.resp.txt");
        client.get("/REST/v3/EPBDS-9622/doSomething", "/EPBDS-9622/EPBDS-9622_v3.resp.txt");
    }

    @Test
    public void EPBDS_9576() {
        client.post("/REST/EPBDS-9576/mySpr",
            "/EPBDS-9576/EPBDS-9576_mySpr.req.json",
            "/EPBDS-9576/EPBDS-9576_mySpr.resp.json");
        client.get("/REST/EPBDS-9576/swagger.json", "/EPBDS-9576/EPBDS-9576_swagger.resp.json");
        client.get("/REST/EPBDS-9576/openapi.json", "/EPBDS-9576/EPBDS-9576_openapi.resp.json");
    }

    @Test
    public void EPBDS_9665() {
        client.post("/EPBDS-9665/EPBDS-9665/anotherSpr",
            "/EPBDS-9665/EPBDS-9665_anotherSpr.req.json",
            "/EPBDS-9665/EPBDS-9665_anotherSpr.resp.txt");
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
    public void EPBDS_9764() {
        client.get("/REST/EPBDS-9764/EPBDS-9764/swagger.json", "/EPBDS-9764/EPBDS-9764_swagger.resp.json");
        client.get("/REST/EPBDS-9764/EPBDS-9764/openapi.json", "/EPBDS-9764/EPBDS-9764_openapi.resp.json");
    }

    @Test
    public void EPBDS_9928() {
        client.get("/EPBDS-9928-rs/swagger.json", "/EPBDS-9928/EPBDS-9928_swagger.resp.json");
        client.get("/EPBDS-9928-rs/openapi.json", "/EPBDS-9928/EPBDS-9928_openapi.resp.json");
    }

    @Test
    public void EPBDS_10027() {
        client.get("/REST/EPBDS-10027/EPBDS-10027/swagger.json", "/EPBDS-10027/EPBDS-10027_swagger.resp.json");
        client.get("/REST/EPBDS-10027/EPBDS-10027/openapi.json", "/EPBDS-10027/EPBDS-10027_openapi.resp.json");
        client.post("/REST/EPBDS-10027/EPBDS-10027/nonEnglishLangs",
            "/EPBDS-10027/EPBDS-10027.req.json",
            "/EPBDS-10027/EPBDS-10027.resp.json");
    }

    @Test
    public void EPBDS_10118() {
        client.get("/REST/EPBDS-10118/EPBDS-10118/swagger.json", "/EPBDS-10118/EPBDS-10118_swagger.resp.json");
        client.get("/REST/EPBDS-10118/EPBDS-10118/openapi.json", "/EPBDS-10118/EPBDS-10118_openapi.resp.json");
    }

    @Test
    public void EPBDS_10171() {
        client.get("/EPBDS-10171/EPBDS-10171/swagger.json", "/EPBDS-10171/EPBDS-10171_swagger.resp.json");
        client.get("/EPBDS-10171/EPBDS-10171/openapi.json", "/EPBDS-10171/EPBDS-10171_openapi.resp.json");
        client.post("/EPBDS-10171/EPBDS-10171/homeRule2",
            "/EPBDS-10171/EPBDS-10171.req.txt",
            "/EPBDS-10171/EPBDS-10171.resp.json");
    }

    @Test
    public void EPBDS_5057() {
        client.post("/REST/КириллицаТест/hello", "/EPBDS-5057/EPBDS-5057.req.json", 200);
    }

    @Test
    public void EPBDS_6555() {
        client.post("/REST/EPBDS-6555/Greeting",
            "/EPBDS-6555/EPBDS-6555_Greeting.req.json",
            "/EPBDS-6555/EPBDS-6555_Greeting.resp.txt");
        client.post("/REST/EPBDS-6555/Calc",
            "/EPBDS-6555/EPBDS-6555_Calc.req.json",
            "/EPBDS-6555/EPBDS-6555_Calc.resp.txt");
    }

    @Test
    public void EPBDS_7187() {
        client.post("/upcs/lowCase",
            "/EPBDS-7187/EPBDS_7187_low-case.req.json",
            "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.post("/upcs/UPCase",
            "/EPBDS-7187/EPBDS_7187_upper-case.req.json",
            "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.post("/upcs/MixedCase",
            "/EPBDS-7187/EPBDS_7187_mixed-case.req.json",
            "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.post("/upcs/eDGECase",
            "/EPBDS-7187/EPBDS_7187_edge-case.req.json",
            "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.get("/upcs/overload", "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.get("/upcs/overload/1", "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.post("/upcs/overload3",
            "/EPBDS-7187/EPBDS_7187_overload-4.req.json",
            "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.post("/upcs/overload2",
            "/EPBDS-7187/EPBDS_7187_overload-3.req.json",
            "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.post("/upcs/overload1",
            "/EPBDS-7187/EPBDS_7187_overload-1.req.json",
            "/EPBDS-7187/EPBDS_7187_true-value.resp.txt");
        client.post("/upcs/mixes",
            "/EPBDS-7187/EPBDS_7187_empty.req.json",
            "/EPBDS-7187/EPBDS_7187_mixes1-default.resp.json");
        client.post("/upcs/mixes",
            "/EPBDS-7187/EPBDS_7187_mixes1-filled-nulls.json",
            "/EPBDS-7187/EPBDS_7187_mixes1-filled-nulls.json");
        client.post("/upcs/mixes",
            "/EPBDS-7187/EPBDS_7187_mixes1-filled.json",
            "/EPBDS-7187/EPBDS_7187_mixes1-filled.json");
        client.post("/upcs/mixes2",
            "/EPBDS-7187/EPBDS_7187_empty.req.json",
            "/EPBDS-7187/EPBDS_7187_mixes2-empty.resp.json");
        client.post("/upcs/mixes2",
            "/EPBDS-7187/EPBDS_7187_mixes2-default.req.json",
            "/EPBDS-7187/EPBDS_7187_mixes2-default.resp.json");
        client.post("/upcs/mixes2",
            "/EPBDS-7187/EPBDS_7187_mixes2-filled.req.json",
            "/EPBDS-7187/EPBDS_7187_mixes2-filled.resp.json");
        client.get("/upcs/swagger.json", "/EPBDS-7187/EPBDS_7187_swagger.resp.json");
        client.get("/upcs/openapi.json", "/EPBDS-7187/EPBDS_7187_openapi.resp.json");
    }

    @Test
    public void EPBDS_7654() {
        client.send("EPBDS-7654/EPBDS-7654_dayDiff");
        client.send("EPBDS-7654/EPBDS-7654_dayDiff-wrong");
    }

    @Test
    public void EPBDS_7787() {
        client.post("/EPBDS-7787-project1/calculation",
            "/EPBDS-7787/EPBDS-7787_calc.req.json",
            "/EPBDS-7787/EPBDS-7787_calc.resp.json");
        client.get("/EPBDS-7787-project1/getProject2FirstPolicy", "/EPBDS-7787/EPBDS-7787_proj2.resp.json");
        client.get("/EPBDS-7787-project1/getProject1FirstPolicy", "/EPBDS-7787/EPBDS-7787_proj1.resp.json");
    }

    @Test
    public void EPBDS_7947_test_validation_shouldBeOK() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/EPBDS-7947/validation_policy.req.json",
            "/EPBDS-7947/validation_shouldBeOK_OK.resp.txt");
        client.post("/REST/parent-datatype-validation/checkArrayValidation",
            "/EPBDS-7947/validation_shouldBeOK_policies.req.json",
            "/EPBDS-7947/validation_shouldBeOK_OK.resp.txt");
        client.post("/REST/parent-datatype-validation/checkArrayValidationFromParent",
            "/EPBDS-7947/validation_shouldBeOK_policies.req.json",
            "/EPBDS-7947/validation_shouldBeOK_OK.resp.txt");
        client.post("/REST/parent-datatype-validation/getGender",
            "/EPBDS-7947/validation_shouldBeOK_gender.req.txt",
            "/EPBDS-7947/validation_shouldBeOK_gender.resp.txt");
    }

    @Test
    public void EPBDS_7947_test_validation_onPolicy_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/EPBDS-7947/validation_onPolicy_shouldBeFailed.req.json",
            422,
            "/EPBDS-7947/validation_onPolicy_shouldBeFailed.resp.json");
    }

    @Test
    public void EPBDS_7947_test_validation_onDriver_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/EPBDS-7947/validation_onDriver_shouldBeFailed.req.json",
            422,
            "/EPBDS-7947/validation_onDriver_shouldBeFailed.resp.json");
    }

    @Test
    public void EPBDS_7947_test_validation_onCoverage_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/EPBDS-7947/validation_onCoverage_shouldBeFailed.req.json",
            422,
            "/EPBDS-7947/validation_onCoverage_shouldBeFailed.resp.json");
    }

    @Test
    public void EPBDS_7947_test_validation_onBrandCode_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/EPBDS-7947/validation_onBrandCode_shouldBeFailed.req.json",
            422,
            "/EPBDS-7947/validation_onBrandCode_shouldBeFailed.resp.json");
    }

    @Test
    public void EPBDS_7947_test_validation_getGender_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/getGender",
            "/EPBDS-7947/validation_getGender_shouldBeFailed.req.txt",
            422,
            "/EPBDS-7947/validation_getGender_shouldBeFailed.resp.json");
    }

    @Test
    public void EPBDS_7947_test_validation_onArrays_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkArrayValidation",
            "/EPBDS-7947/validation_onArrays_shouldBeFailed.req.json",
            422,
            "/EPBDS-7947/validation_onArrays_shouldBeFailed.resp.json");
        client.post("/REST/parent-datatype-validation/checkArrayValidationFromParent",
            "/EPBDS-7947/validation_onArrays_shouldBeFailed.req.json",
            422,
            "/EPBDS-7947/validation_onArrays_shouldBeFailed.resp.json");
    }

    @Test
    public void EPBDS_7947_test_validation_onPaymentMatrix_shouldBeFailed() {
        client.send("EPBDS-7947/validation_onPaymentMatrix_shouldBeFailed");
    }

    @Test
    public void EPBDS_8076() {
        client.post("/REST/EPBDS-8076/EPBDS-8076/m1", "/EPBDS-8076/empty.req.json", "/EPBDS-8076/const1.resp.txt");
        client.post("/REST/EPBDS-8076/EPBDS-8076/m2", "/EPBDS-8076/empty.req.json", "/EPBDS-8076/const2.resp.txt");
        client.post("/REST/EPBDS-8076/EPBDS-8076/s1", "/EPBDS-8076/empty.req.json", "/EPBDS-8076/const1.resp.txt");
        client.post("/REST/EPBDS-8076/EPBDS-8076/s2", "/EPBDS-8076/empty.req.json", "/EPBDS-8076/const2.resp.txt");
        client.post("/REST/EPBDS-8076/EPBDS-8076/t1", "/EPBDS-8076/empty.req.json", "/EPBDS-8076/const1.resp.txt");
        client.post("/REST/EPBDS-8076/EPBDS-8076/t2", "/EPBDS-8076/empty.req.json", "/EPBDS-8076/const2.resp.txt");
    }

    @Test
    public void EPBDS_7757_testHttpStatuses() {
        client.get("/REST/http-statuses-test/throwUserException", 422, "/EPBDS-7757/rest_USER_ERROR_response.json");
        client.get("/REST/http-statuses-test/throwOpenLException", 500, "/EPBDS-7757/rest_RULES_RUNTIME_response.json");
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
    }

    @Test
    public void EPBDS_10366() {
        client.get("/EPBDS-10366/swagger.json", "/EPBDS-10366/EPBDS-10366_swagger.resp.json");
        client.get("/EPBDS-10366/openapi.json", "/EPBDS-10366/EPBDS-10366_openapi.resp.json");
        client.get("/EPBDS-10366/check", "/EPBDS-10366/EPBDS-10366.resp.json");
    }

    @Test
    public void EPBDS_10393() {
        client.get("/EPBDS-10393/swagger.json", "/EPBDS-10393/EPBDS-10393_swagger.resp.json");
        client.get("/EPBDS-10393/openapi.json", "/EPBDS-10393/EPBDS-10393_openapi.resp.json");
        client.get("/EPBDS-10393/someRule5", "/EPBDS-10393/EPBDS-10393_someRule5.resp.json");
    }

    @Test
    public void EPBDS_10393_2() {
        client.get("/EPBDS-10393_2/swagger.json", "/EPBDS-10393_2/EPBDS-10393_2_swagger.resp.json");
        client.get("/EPBDS-10393_2/openapi.json", "/EPBDS-10393_2/EPBDS-10393_2_openapi.resp.json");
    }

    @Test
    public void EPBDS_10393_3() {
        client.post("/EPBDS-10393_3/test1",
            "/EPBDS-10393_3/EPBDS-10393_3_test1.req.json",
            "/EPBDS-10393_3/EPBDS-10393_3_test1.resp.json");
    }

    @Test
    public void EPBDS_10483() {
        client.get("/EPBDS-10483/EPBDS-10483/swagger.json", "/EPBDS-10483/EPBDS-10483_swagger.resp.json");
        client.get("/EPBDS-10483/EPBDS-10483/openapi.json", "/EPBDS-10483/EPBDS-10483_openapi.resp.json");
    }

    @Test
    public void EPBDS_10557() {
        client.post("/EPBDS-10557/EPBDS-10557/myRule1",
            "/EPBDS-10557/EPBDS-10557_myRule1.req.json",
            "/EPBDS-10557/EPBDS-10557_myRule1.resp.json");
    }

    @Test
    public void EPBDS_10595() {
        client.get("/EPBDS-10595/EPBDS-10595/swagger.json", "/EPBDS-10595/EPBDS-10595_swagger.resp.json");
        client.get("/EPBDS-10595/EPBDS-10595/openapi.json", "/EPBDS-10595/EPBDS-10595_openapi.resp.json");
    }

    @Test
    public void EPBDS_10699() {
        client.get("/admin/services/EPBDS-10699/EPBDS-10699/errors", "/EPBDS-10699/EPBDS-10699_error.resp.json");
    }

    @Test
    public void EPBDS_10708() {
        client.get("/EPBDS-10708/EPBDS-10708/swagger.json", "/EPBDS-10708/EPBDS-10708_swagger.resp.json");
        client.get("/EPBDS-10708/EPBDS-10708/openapi.json", "/EPBDS-10708/EPBDS-10708_openapi.resp.json");
        client.get("/EPBDS-10708/EPBDS-10708/mainSpr", "/EPBDS-10708/EPBDS-10708_mainSpr.resp.json");
    }

    @Test
    public void EPBDS_10743() {
        client.get("/EPBDS-10743/EPBDS-10743/swagger.json", "/EPBDS-10743/EPBDS-10743_swagger.resp.json");
        client.get("/EPBDS-10743/EPBDS-10743/openapi.json", "/EPBDS-10743/EPBDS-10743_openapi.resp.json");
    }

    @Test
    public void EPBDS_8931() {
        client.send("EPBDS-8931/openapi.json.get");
        client.send("EPBDS-8931/swagger.json.get");
    }

    @Test
    public void EPBDS_10996() {
        client.send("EPBDS-10996/openapi.json.get");
        client.send("EPBDS-10996/swagger.json.get");
    }

    @Test
    public void test_missed_annotation_template_class() {
        client.send("missed_annotation_template_class/errors.get");
    }

    @Test
    public void EPBDS_11123() {
        client.get("/EPBDS-11123/Project1/mySpr/3", "/EPBDS-11123/EPBDS-11123_mySpr.resp.json");
    }

    @Test
    public void EPBDS_11682() {
        client.send("EPBDS-11682/openapi.json.get");
        client.send("EPBDS-11682/swagger.json.get");
        client.send("EPBDS-11682/VoidResponse.json.post");
        client.send("EPBDS-11682/VoidResponse2.json.post");
        client.send("EPBDS-11682/IntegerResponse.json.post");
        client.send("EPBDS-11682/IntegerResponse2.json.post");
        client.send("EPBDS-11682/StringResponse.json.post");
        client.send("EPBDS-11682/StringResponse2.json.post");
        client.send("EPBDS-11682/StringResponse3.json.post");
        client.send("EPBDS-11682/StringValueResponse.json.post");
        client.send("EPBDS-11682/StringValueResponse2.json.post");
        client.send("EPBDS-11682/StringValueResponse3.json.post");
        client.send("EPBDS-11682/IntResponse.json.post");
        client.send("EPBDS-11682/GetOptionalResponse.json.post");
        client.send("EPBDS-11682/GetOptionalResponse2.json.post");
    }

    @Test
    public void EPBDS_11725() {
        client.send("EPBDS-11725/RateCardPremiumAggregated.json.post");
    }

    @Test
    public void EPBDS_11883() {
        client.send("EPBDS-11883/Table1.json.post");
    }

    @Test
    public void EPBDS_12114() {
        client.send("EPBDS-12114/swagger.json.get");
        client.send("EPBDS-12114/openapi.json.get");
        client.send("EPBDS-12114/doPing.json.post");
    }

    @Test
    public void EPBDS_12057() {
        client.send("EPBDS-12057/openapi.json.get");
    }

    @Test
    public void EPBDS_12225() {
        client.send("EPBDS-12225/mainSpr.json.get");
    }

    @Test
    public void EPBDS_12310() {
        client.send("EPBDS-12310/openapi.json.get");
    }

    @Test
    public void EPBDS_12125() {
        client.send("EPBDS-12125/myRule.json.get");
        client.send("EPBDS-12125/myRule2.json.get");
    }

    @Test
    public void EPBDS_12267() {
        client.send("EPBDS-12267/myRules.json.post");
        client.send("EPBDS-12267/mySr2.json.post");
    }

    @Test
    public void EPBDS_12266() {
        client.send("EPBDS-12266/myRules.json.post");
    }

    @Test
    public void EPBDS_12264() {
        client.send("EPBDS-12264/myRules.json.post");
    }

    @Test
    public void EPBDS_12546() {
        client.send("EPBDS-12546/theSpreadsheet.json.get");
        client.send("EPBDS-12546/theSpreadsheet2.json.get");
    }
}
