package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunAnnotationsITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer(true);
        server.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void call_parse_interfaceMethod_shouldBeCalledSuccessfully_OK() {
        client.post("/REST/v1/string/toNumber/parse1",
            "/stringToNumber_parse1.req.txt",
            "/stringToNumber_parse1.resp.txt");
    }

    @Test
    public void call_parse_interfaceMethod_shouldReturn_UNPROCESSABLE_ENTITY() {
        client.post("/REST/v1/string/toNumber/parse1",
            "/stringToNumber_parse1_error.req.txt",
            422,
            "/stringToNumber_parse1_error.resp.json");
    }

    @Test
    public void call_parse1_interfaceMethod_shouldReturn_NOT_FOUND() {
        client.post("/REST/v1/string/toNumber/parse11", "/stringToNumber_parse11.req.txt", 404, "/404.resp.txt");
    }

    @Test
    public void call_parse2_interfaceMethod_thenInvokeAfterInterceptorAndWrapException_NOT_ACCEPTABLE() {
        client.post("/REST/v1/string/toNumber/parse2",
            "/stringToNumber_parse2_notValid.req.txt",
            406,
            "/stringToNumber_parse2_notValid.resp.json");
    }

    @Test
    public void call_parse2_interfaceMethod_thenInvokeAfterInterceptorAndWrapResponse_OK() {
        client.post("/REST/v1/string/toNumber/parse2",
            "/stringToNumber_parse2.req.txt",
            "/stringToNumber_parse2.resp.json");
    }

    @Test
    public void call_parse3_interfaceMethod_usingCustomEndpoint_OK() {
        client.get("/REST/v1/string/toNumber/parse/111", "/stringToNumber_parse.resp.txt");
    }

    @Test
    public void call_parse3_interfaceMethod_usingCustomEndpoint_errorResponseMustBeReturned_UNPROCESSABLE_ENTITY() {
        client.get("/REST/v1/string/toNumber/parse/A", 422, "/stringToNumber_parse_notValid.resp.json");
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_OK() {
        client.post("/REST/v1/string/toNumber/parseX",
            "/stringToNumber_parseX.req.txt",
            "/stringToNumber_parseX.resp.xml");
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldReturnError_OK() {
        client.post("/REST/v1/string/toNumber/parseX",
            "/stringToNumber_parseX_error.req.txt",
            "/stringToNumber_parseX_error.resp.xml");
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldThrowErrorInBeforeInterceptor_OK() {
        client.post("/REST/v1/string/toNumber/parseX",
            "/stringToNumber_parseX_throwBeforeCall.req.txt",
            "/stringToNumber_parseX_throwBeforeCall.resp.xml");
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldReturn_UNSUPPORTED_MEDIA_TYPE() {
        client.post("/REST/v1/string/toNumber/parseX",
            "/stringToNumber_parseX_throwBeforeCall-415.req.json",
            415,
            "/stringToNumber_parseX_throwBeforeCall-415.resp.txt");
    }

    @Test
    public void call_virtual_interfaceMethod_OK() {
        client.post("/REST/v1/string/toNumber/virtual",
            "/stringToNumber_virtual.req.txt",
            "/stringToNumber_virtual.resp.txt");
    }

    @Test
    public void call_virtual_interfaceMethod_UNPROCESSABLE_ENTITY() {
        client.post("/REST/v1/string/toNumber/virtual",
            "/stringToNumber_virtual_error.req.txt",
            422,
            "/stringToNumber_virtual_error.resp.json");
    }

    @Test
    public void call_virtual2_interfaceMethod_withNamedParameters_OK() {
        client.post("/REST/v1/string/toNumber/virtual2",
            "/stringToNumber_virtual2.req.json",
            "/stringToNumber_virtual2.resp.txt");
    }

    @Test
    public void call_pong_interfaceMethod_OK() {
        client.post("/REST/v1/string/toNumber/ping", "/stringToNumber_ping.req.json", "/stringToNumber_ping.resp.json");
    }

    @Test
    public void call_process_rulesMethod_OK() {
        client.post("/REST/v1/string/toNumber/process",
            "/stringToNumber_process.req.json",
            "/stringToNumber_process.resp.json");
    }

    @Test
    public void call__notExcludedBecauseof_p__rulesMethod_OK() {
        client.post("/REST/v1/string/toNumber/notExcludedBecauseof_p_",
            "/stringToNumber_notExculded.req.txt",
            "/stringToNumber_notExculded.resp.txt");
    }

    @Test
    public void call_excluded_rulesMethod_NOT_FOUND() {
        client.post("/REST/v1/string/toNumber/excluded", "/stringToNumber_exculded.req.txt", 404, "/404.resp.txt");
    }

    @Test
    public void test_doSomething_REST() {
        client.post("/REST/ws-serviceclass-positive/doSomething",
            "/serviceclass-positive_doSomething.req.txt",
            "/serviceclass-positive_doSomething.resp.txt");
    }

    @Test
    public void test_doSomething_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_doSomething.req.xml",
            "/serviceclass-positive_doSomething.resp.xml");
    }

    @Test
    public void test_doArray_REST() {
        client.get("/REST/ws-serviceclass-positive/doArray", "/serviceclass-positive_doArray.resp.txt");
    }

    @Test
    public void test_doArray_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_doArray.req.xml",
            "/serviceclass-positive_doArray.resp.xml");
    }

    @Test
    public void test_voidMethod_REST() {
        client.get("/REST/ws-serviceclass-positive/voidMethod");
    }

    @Test
    public void test_voidMethod_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_voidMethod.req.xml",
            "/serviceclass-positive_voidMethod.resp.xml");
    }

    @Test
    public void test_voidMethodWithAfterReturnInterceptor_REST() {
        client.get("/REST/ws-serviceclass-positive/voidMethodWithAfterReturnInterceptor",
            "/serviceclass-positive_voidMethodWithAfterReturnInterceptor.resp.json");
    }

    @Test
    public void test_voidMethodWithAfterReturnInterceptor_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_voidMethodWithAfterReturnInterceptor.req.xml",
            "/serviceclass-positive_voidMethodWithAfterReturnInterceptor.resp.xml");
    }

    @Test
    public void test_longMethodWithAfterReturnInterceptor_REST() {
        client.post("/REST/ws-serviceclass-positive/longMethodWithAfterReturnInterceptor",
            "/serviceclass-positive_longMethodWithAfterReturnInterceptor.req.txt",
            "/serviceclass-positive_longMethodWithAfterReturnInterceptor.resp.json");
    }

    @Test
    public void test_longMethodWithAfterReturnInterceptor_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_longMethodWithAfterReturnInterceptor.req.xml",
            "/serviceclass-positive_longMethodWithAfterReturnInterceptor.resp.xml");
    }

    @Test
    public void test_longMethodWithUpcast_REST() {
        client.post("/REST/ws-serviceclass-positive/longMethodWithUpcast",
            "/serviceclass-positive_longMethodWithUpcast.req.txt",
            "/serviceclass-positive_longMethodWithUpcast.resp.txt");
    }

    @Test
    public void test_longMethodWithUpcast_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_longMethodWithUpcast.req.xml",
            "/serviceclass-positive_longMethodWithUpcast.resp.xml");
    }

    @Test
    public void test_aroundLongMethod_REST() {
        client.post("/REST/ws-serviceclass-positive/aroundLongMethod",
            "/serviceclass-positive_aroundLongMethod.req.txt",
            "/serviceclass-positive_aroundLongMethod.resp.json");
    }

    @Test
    public void test_aroundLongMethod_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_aroundLongMethod.req.xml",
            "/serviceclass-positive_aroundLongMethod.resp.xml");
    }

    @Test
    public void test_aroundVoidMethod_REST() {
        client.get("/REST/ws-serviceclass-positive/aroundVoidMethod",
            "/serviceclass-positive_aroundVoidMethod.resp.json");
    }

    @Test
    public void test_aroundVoidMethod_SOAP() {
        client.post("/ws-serviceclass-positive",
            "/serviceclass-positive_aroundVoidMethod.req.xml",
            "/serviceclass-positive_aroundVoidMethod.resp.xml");
    }

    @Test
    public void test_doSomething_negative_REST() {
        client.post("/REST/ws-serviceclass-negative/doSomething",
            "/serviceclass-negative_doSomething.req.txt",
            404,
            "/404.resp.html");
    }

    @Test
    public void test_runTestTables_calculatePremium_REST() {
        client.post("/REST/rules-tests-and-run-tables/calculatePremium",
            "/runTestTables_calculatePremium.req.json",
            "/runTestTables_calculatePremium.resp.json");
    }

    @Test
    public void test_runTestTables_calculatePremium_ValidationError_REST() {
        client.post("/REST/rules-tests-and-run-tables/calculatePremium",
            "/runTestTables_calculatePremium_notValid.req.json",
            422,
            "/runTestTables_calculatePremium_notValid.resp.json");
    }

    @Test
    public void test_runTestTables_calculatePremiumTest_REST() {
        client.get("/REST/rules-tests-and-run-tables/calculatePremiumTest", 404, "/404.resp.txt");
    }

    @Test
    public void test_runTestTables_getFactor_REST() {
        client.post("/REST/rules-tests-and-run-tables/getFactor", "/404.req.txt", 404, "/404.resp.txt");
    }

    @Test
    public void test_runTestTables_getDiscount_REST() {
        client.post("/REST/rules-tests-and-run-tables/getDiscount", "/404.req.txt", 404, "/404.resp.txt");
    }

    @Test
    public void test_runTestTables_calculatePremiumRun_REST() {
        client.post("/REST/rules-tests-and-run-tables/calculatePremiumRun", "/404.req.txt", 404, "/404.resp.txt");
    }

    @Test
    public void typeChangingToGenericTypeTest() {
        client.get("/v1/string/toNumber?wsdl", "/stringToNumber_wsdl.resp.xml");
        client.get("/REST/v1/string/toNumber?_wadl", "/stringToNumber_wadl.resp.xml");
    }
}
