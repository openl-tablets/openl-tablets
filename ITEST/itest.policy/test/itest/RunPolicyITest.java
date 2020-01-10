package itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunPolicyITest {

    private static JettyServer server;
    private static HttpClient client;

    private static final String SECURITY_MANAGER_KEY = "java.security.manager";
    private static final String SECURITY_POLICY_KEY = "java.security.policy";
    private static String securityManagerValue;
    private static String securityPolicyValue;
    private static SecurityManager oldManager;

    @BeforeClass
    public static void setUp() throws Exception {
        oldManager = System.getSecurityManager();
        securityManagerValue = System.getProperty(SECURITY_MANAGER_KEY);
        securityPolicyValue = System.getProperty(SECURITY_POLICY_KEY);
        System.setProperty(SECURITY_MANAGER_KEY, "");
        System.setProperty(SECURITY_POLICY_KEY, System.getProperty("user.dir") + "/test-resources/iTest.policy");
        System.setSecurityManager(new SecurityManager());
        server = JettyServer.start();
        server.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (securityManagerValue != null) {
            System.setProperty(SECURITY_MANAGER_KEY, securityManagerValue);
        } else {
            System.clearProperty(SECURITY_MANAGER_KEY);
        }
        if (securityPolicyValue != null) {
            System.setProperty(SECURITY_POLICY_KEY, securityPolicyValue);
        } else {
            System.clearProperty(SECURITY_POLICY_KEY);
        }
        System.setSecurityManager(oldManager);
        server.stop();
    }

    @Test
    public void test() {

        client.post("/admin/deploy", "/testSuccess.zip", 201);
        client.post("/REST/test1/create", "empty.json", 200);
        client.post("/REST/test1/removeFail", "empty.json", 500);
        client.post("/REST/test1/remove", "empty.json", 200);
    }

}
