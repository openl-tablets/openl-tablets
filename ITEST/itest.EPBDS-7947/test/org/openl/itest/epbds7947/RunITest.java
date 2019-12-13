package org.openl.itest.epbds7947;

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
    public void test_validation_shouldBeOK() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/validation_policy.req.json",
            "/validation_shouldBeOK_OK.resp.txt");
        client.post("/REST/parent-datatype-validation/checkArrayValidation",
            "/validation_shouldBeOK_policies.req.json",
            "/validation_shouldBeOK_OK.resp.txt");
        client.post("/REST/parent-datatype-validation/checkArrayValidationFromParent",
            "/validation_shouldBeOK_policies.req.json",
            "/validation_shouldBeOK_OK.resp.txt");
        client.post("/REST/parent-datatype-validation/getGender",
            "/validation_shouldBeOK_gender.req.txt",
            "/validation_shouldBeOK_gender.resp.txt");

        client.post("/parent-datatype-validation", "/validation_policy.req.xml", "/validation_policy.resp.xml");
        client.post("/parent-datatype-validation",
            "/validation_shouldBeOK_policies.req.xml",
            "/validation_shouldBeOK_policies.resp.xml");
        client.post("/parent-datatype-validation",
            "/validation_shouldBeOK_gender.req.xml",
            "/validation_shouldBeOK_gender.resp.xml");
    }

    @Test
    public void test_validation_onPolicy_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/validation_onPolicy_shouldBeFailed.req.json",
            422,
            "/validation_onPolicy_shouldBeFailed.resp.json");

        client.post("/parent-datatype-validation",
            "/validation_onPolicy_shouldBeFailed.req.xml",
            500,
            "/validation_onPolicy_shouldBeFailed.resp.xml");
    }

    @Test
    public void test_validation_onDriver_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/validation_onDriver_shouldBeFailed.req.json",
            422,
            "/validation_onDriver_shouldBeFailed.resp.json");
    }

    @Test
    public void test_validation_onCoverage_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/validation_onCoverage_shouldBeFailed.req.json",
            422,
            "/validation_onCoverage_shouldBeFailed.resp.json");

        client.post("/parent-datatype-validation",
            "/validation_onCoverage_shouldBeFailed.req.xml",
            500,
            "/validation_onCoverage_shouldBeFailed.resp.xml");
    }

    @Test
    public void test_validation_onBrandCode_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkValidation",
            "/validation_onBrandCode_shouldBeFailed.req.json",
            422,
            "/validation_onBrandCode_shouldBeFailed.resp.json");

        client.post("/parent-datatype-validation",
            "/validation_onBrandCode_shouldBeFailed.req.xml",
            500,
            "/validation_onBrandCode_shouldBeFailed.resp.xml");
    }

    @Test
    public void test_validation_getGender_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/getGender",
            "/validation_getGender_shouldBeFailed.req.txt",
            422,
            "/validation_getGender_shouldBeFailed.resp.json");
        client.post("/parent-datatype-validation",
            "/validation_getGender_shouldBeFailed.req.xml",
            500,
            "/validation_getGender_shouldBeFailed.resp.xml");
    }

    @Test
    public void test_validation_onArrays_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkArrayValidation",
            "/validation_onArrays_shouldBeFailed.req.json",
            422,
            "/validation_onArrays_shouldBeFailed.resp.json");
        client.post("/REST/parent-datatype-validation/checkArrayValidationFromParent",
            "/validation_onArrays_shouldBeFailed.req.json",
            422,
            "/validation_onArrays_shouldBeFailed.resp.json");

        client.post("/parent-datatype-validation",
            "/validation_onArrays_shouldBeFailed.req.xml",
            500,
            "/validation_onArrays_shouldBeFailed.resp.xml");

        client.post("/parent-datatype-validation",
            "/validation_onArraysFromParent_shouldBeFailed.req.xml",
            500,
            "/validation_onArrays_shouldBeFailed.resp.xml");
    }

    @Test
    public void test_validation_onPaymentMatrix_shouldBeFailed() {
        client.post("/REST/parent-datatype-validation/checkArrayValidationFromParent",
            "/validation_onPaymentMatrix_shouldBeFailed.req.json",
            400,
            "/validation_onPaymentMatrix_shouldBeFailed.resp.json");

        client.post("/parent-datatype-validation",
            "/validation_onPaymentMatrix_shouldBeFailed.req.xml",
            500,
            "/validation_onPaymentMatrix_shouldBeFailed.resp.xml");
    }
}
