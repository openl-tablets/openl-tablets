package org.openl.rules.ruleservice.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/RulesFrontendTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml", "classpath:OpenLServiceFactoryBeanTest.xml" })
public class OpenLServiceFactoryBeanTest {

    @Resource
    private ServiceInterface ruleService1;

    @Resource
    private ServiceInterface ruleService2;

    @Test
    public void testDefaultFrontend() {
        assertNotNull(ruleService1);
        assertEquals("World, Good Morning!", ruleService1.worldHello(10));
        assertEquals("i: 10 s: 5", ruleService1.worldHello(10, "5"));
        assertEquals("i: null s: null", ruleService1.worldHello(null, null));
        assertEquals("i: null s: 5", ruleService1.worldHello(null, "5"));
    }

    @Test
    public void testDefaultFrontendAbsentMethods() {
        try {
            ruleService1.worldHello(null);
            fail();
        } catch (Exception e) {
            assertEquals("org.openl.rules.ruleservice.simple.MethodInvocationException: Method 'worldHello(java.lang.String)' is not found in service 'RulesFrontendTest_multimodule'.", e.getMessage());
        }
    }

    @Test
    public void testDefaultFrontendAbsentMethods2() {
        try {
            ruleService1.absent(null);
            fail();
        } catch (Exception e) {
            assertEquals("org.openl.rules.ruleservice.simple.MethodInvocationException: Method 'absent(java.lang.String)' is not found in service 'RulesFrontendTest_multimodule'.", e.getMessage());
        }
    }

    @Test(expected = org.openl.rules.ruleservice.simple.MethodInvocationRuntimeException.class)
    public void testAbsentMethods() {
        ruleService1.absent("X");
    }

    @Test
    public void testOverridedFrontend() {
        assertEquals("Invoked: worldHello 1 int", ruleService2.worldHello(10));
        assertEquals("Invoked: worldHello 2 Integer", ruleService2.worldHello(10, "5"));
        assertEquals("Invoked: worldHello 2 Integer", ruleService2.worldHello(null, null));
        assertEquals("Invoked: worldHello 2 Integer", ruleService2.worldHello(null, "5"));
        assertEquals("Invoked: worldHello 1 String", ruleService2.worldHello(null));
        assertEquals("Invoked: absent 1 String", ruleService2.absent(null));
    }

    public interface ServiceInterface {
        String worldHello(int arg);
        String worldHello(Integer a, String s);
        String worldHello(String s);
        String absent(String s);
    }

    public static abstract class FrontendImpl implements RulesFrontend {

        @Override
        public Object execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object... params) throws MethodInvocationException {
            return "Invoked: " + ruleName + " " + inputParamsTypes.length + " " + inputParamsTypes[0].getSimpleName();
        }
    }
}
