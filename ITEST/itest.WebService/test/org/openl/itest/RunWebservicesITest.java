package org.openl.itest;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
    public void testWadlSchemaSimple1() {
        client.get("/REST/deployment1/simple1?_wadl", "/simple1_wadl.resp.xml");
    }

    @Test
    public void testWsdlSchemaSimple1() {
        client.get("/deployment1/simple1?wsdl", "/simple1_wsdl.resp.xml");
    }

    @Test
    public void testSimple1_invoke_with_Variations() {
        client
            .post("/REST/deployment1/simple1/test1", "/simple1_invoke_test.req.json", "/simple1_invoke_test.resp.json");
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
    public void testSwaggerSchemaSimple3() {
        client.get("/REST/deployment3/simple3/swagger.json", "/simple3_swagger.resp.json");
        client.get("/REST/deployment3/simple3/openapi.json", "/simple3_openapi.resp.json");
    }

    @Test
    public void testWADLSchemaSimple3() {
        client.get("/REST/deployment3/simple3?_wadl", "/simple3_wadl.resp.xml");
    }

    @Test
    @Ignore("EPBDS-9728 Ignored because of unstable WSDL schema generation for ArrayOfAnyType.")
    /**
     * NOTE The result of
     * {@link org.apache.cxf.aegis.type.collection.CollectionType#getComponentType()#isNillable()}always {@code true}.
     * But it's {@code false} for {@link org.apache.cxf.aegis.type.basic.ArrayType#getComponentType()}.</br>
     * It may give a different result that depends on position of it in
     * {@link org.apache.cxf.aegis.databinding.AegisDatabinding#createSchemas(...)} in local HashMap {@code tns2Type}.
     */
    public void testWSDLSchemaSimple3() {
        client.get("/deployment3/simple3?wsdl", "/simple3_wsdl.resp.xml");
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
        client.post("/REST/deployment4/simple4/main",
            "/simple3_main.req.json",
            "/simple4_main_response_serialization_inclusion.json");
    }

    @Test
    public void testSimple3_CSPR_Convert_2() {
        client.post("/deployment3/simple3", "/simple3_main.req.xml", "/simple3_main.resp.xml");
    }

    @Test
    public void testWadlSchemaSimple5() {
        client.get("/REST/deployment5/simple5?_wadl", "/simple5_wadl.resp.xml");
    }

    @Test
    public void testSwaggerSchemaSimple5() {
        client.get("/REST/deployment5/simple5/swagger.json", "/simple5_swagger.resp.json");
        client.get("/REST/deployment5/simple5/openapi.json", "/simple5_openapi.resp.json");
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
        client.get("/REST/EPBDS-9519_2/EPBDS-9519/swagger.json", "/EPBDS-9519/EPBDS-9519_2_swagger.resp.json");
        client.get("/REST/EPBDS-9519_2/EPBDS-9519/openapi.json", "/EPBDS-9519/EPBDS-9519_2_openapi.resp.json");
    }

    @Test
    public void EPBDS_9519_3() {
        client.get("/REST/EPBDS-9519_3/EPBDS-9519/swagger.json", "/EPBDS-9519/EPBDS-9519_3_swagger.resp.json");
        client.get("/REST/EPBDS-9519_3/EPBDS-9519/openapi.json", "/EPBDS-9519/EPBDS-9519_3_openapi.resp.json");
    }

    @Test
    public void EPBDS_9572() {
        client.get("/REST/EPBDS-9572/EPBDS-9572/swagger.json", "/EPBDS-9572/EPBDS-9572_swagger.resp.json");
        client.get("/REST/EPBDS-9572/EPBDS-9572/openapi.json", "/EPBDS-9572/EPBDS-9572_openapi.resp.json");
    }

    @Test
    public void EPBDS_9581() {
        client.get("/EPBDS-9581/EPBDS-9581/swagger.json", "/EPBDS-9581/EPBDS-9581_swagger.resp.json");
        client.get("/EPBDS-9581/EPBDS-9581/openapi.json", "/EPBDS-9581/EPBDS-9581_openapi.resp.json");
        client.get("/EPBDS-9581/EPBDS-9581?_wadl", 404);
    }

    @Test
    public void EPBDS_9453() {
        client.post("/EPBDS-9453/EPBDS-9453/proxyCustomer",
            "/EPBDS-9453/EPBDS-9453_proxyCustomer.req.json",
            "/EPBDS-9453/EPBDS-9453_proxyCustomer.resp.json");

    }

    @Test
    public void EPBDS_9619() {
        client.get("/REST/EPBDS-9619/EPBDS-9619/swagger.json", "/EPBDS-9619/EPBDS-9619_swagger.resp.json");
        client.get("/REST/EPBDS-9619/EPBDS-9619/openapi.json", "/EPBDS-9619/EPBDS-9619_openapi.resp.json");
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
        client.get("/REST/EPBDS-9576?_wadl", "/EPBDS-9576/EPBDS-9576_wadl.resp.xml");
        client.get("/EPBDS-9576?wsdl", "/EPBDS-9576/EPBDS-9576_wsdl.resp.xml");
    }

    @Test
    public void EPBDS_9665() {
        client.post("/REST/EPBDS-9665/EPBDS-9665/anotherSpr",
            "/EPBDS-9665/EPBDS-9665_anotherSpr.req.json",
            "/EPBDS-9665/EPBDS-9665_anotherSpr.resp.txt");
    }

    @Test
    public void EPBDS_9678() {
        client.post("/REST/EPBDS-9678/EPBDS-9678/someRule", "/EPBDS-9678/EPBDS-9678_someRule.req.json", 404);
        client.get("/admin/services/EPBDS-9678_EPBDS-9678/errors", "/EPBDS-9678/EPBDS-9678_comopilation_errors.json");

        client.get("/admin/services/EPBDS-9678-project1/errors",
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
    public void EPBDS_9764_2() {
        client.get("/EPBDS-9764/EPBDS-9764?wsdl", "/EPBDS-9764/EPBDS-9764_wsdl.resp.xml");
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
        client.get("/REST/EPBDS-10027/EPBDS-10027?_wadl", "/EPBDS-10027/EPBDS-10027_wadl.resp.xml");
        client.get("/EPBDS-10027/EPBDS-10027?wsdl", "/EPBDS-10027/EPBDS-10027_wsdl.resp.xml");
        client.post("/REST/EPBDS-10027/EPBDS-10027/nonEnglishLangs",
            "/EPBDS-10027/EPBDS-10027.req.json",
            "/EPBDS-10027/EPBDS-10027.resp.json");
    }

    @Test
    public void EPBDS_10118() {
        client.get("/REST/EPBDS-10118/EPBDS-10118/swagger.json", "/EPBDS-10118/EPBDS-10118_swagger.resp.json");
        client.get("/REST/EPBDS-10118/EPBDS-10118/openapi.json", "/EPBDS-10118/EPBDS-10118_openapi.resp.json");
        client.get("/EPBDS-10118/EPBDS-10118?wsdl", "/EPBDS-10118/EPBDS-10118_wsdl.resp.xml");
    }
}
