package org.openl.itest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.generated.my1.Policy;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.SoapClientFactory;
import org.openl.itest.project1.Project1Service;
import org.openl.rules.calc.SpreadsheetResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;

    private Project1Service project1Service;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer(true);
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        project1Service = new SoapClientFactory<>(baseURI + "/EPBDS-7787-project1", Project1Service.class).createProxy();
    }

    @Test
    public void testMethodFromFirstProject() {
        Policy project1Policy = new Policy();
        project1Policy.setPolicyAmount(1);
        SpreadsheetResult result = project1Service.calculation(project1Policy);
        assertNotNull(result);
        assertEquals(11, result.getFieldValue("$Value$Step2"));
    }

    @Test
    public void testMethodFromProject2_usingUsingInterfaceFromProject1() {
        org.openl.generated.my2.Policy project2Policy = project1Service.getProject2FirstPolicy();
        assertNotNull(project2Policy);
        assertEquals("Policy1", project2Policy.getPolicyTile());
        assertEquals(new Integer(100), project2Policy.getPolicyAmount());
    }

    @Test
    public void testMethodFromProject1_usingUsingInterfaceFromProject1() {
        Policy project2Policy = project1Service.getProject1FirstPolicy();
        assertNotNull(project2Policy);
        assertEquals("Policy1", project2Policy.getPolicyName());
        assertEquals(new Integer(101), project2Policy.getPolicyAmount());
    }

}
