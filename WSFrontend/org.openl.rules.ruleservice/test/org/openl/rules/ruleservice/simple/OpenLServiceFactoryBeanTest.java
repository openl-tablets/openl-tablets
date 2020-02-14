package org.openl.rules.ruleservice.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/RulesFrontendTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml", "classpath:OpenLServiceFactoryBeanTest.xml" })
public class OpenLServiceFactoryBeanTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        this.applicationContext = arg0;
    }

    @Test
    public void testDefaultFrontend() {
        assertNotNull(applicationContext);
        ServiceInterface service = applicationContext.getBean("ruleService1", ServiceInterface.class);
        assertNotNull(service);
        assertEquals("World, Good Morning!", service.worldHello(10));
        assertEquals("i: 10 s: 5", service.worldHello(10, "5"));
        assertEquals("i: null s: null", service.worldHello(null, null));
        assertEquals("i: null s: 5", service.worldHello(null, "5"));
    }

    @Test
    public void testDefaultFrontendAbsentMethods() {
        assertNotNull(applicationContext);
        ServiceInterface service = applicationContext.getBean("ruleService1", ServiceInterface.class);
        assertNotNull(service);
        try {
            service.worldHello(null);
            fail();
        } catch (Exception e) {
            assertEquals("org.openl.rules.ruleservice.simple.MethodInvocationException: Method 'worldHello(java.lang.String)' is not found in service 'RulesFrontendTest_multimodule'.", e.getMessage());
        }
    }

    @Test
    public void testDefaultFrontendAbsentMethods2() {
        assertNotNull(applicationContext);
        ServiceInterface service = applicationContext.getBean("ruleService1", ServiceInterface.class);
        assertNotNull(service);
        try {
            service.absent(null);
            fail();
        } catch (Exception e) {
            assertEquals("org.openl.rules.ruleservice.simple.MethodInvocationException: Method 'absent(java.lang.String)' is not found in service 'RulesFrontendTest_multimodule'.", e.getMessage());
        }
    }

    @Test
    public void testOverridedFrontend() {
        assertNotNull(applicationContext);
        ServiceInterface service = applicationContext.getBean("ruleService2", ServiceInterface.class);
        assertNotNull(service);
        assertEquals("Invoked: worldHello 1 int", service.worldHello(10));
        assertEquals("Invoked: worldHello 2 Integer", service.worldHello(10, "5"));
        assertEquals("Invoked: worldHello 2 Integer", service.worldHello(null, null));
        assertEquals("Invoked: worldHello 2 Integer", service.worldHello(null, "5"));
        assertEquals("Invoked: worldHello 1 String", service.worldHello(null));
        assertEquals("Invoked: absent 1 String", service.absent(null));
    }

    public interface ServiceInterface {
        String worldHello(int arg);
        String worldHello(Integer a, String s);
        String worldHello(String s);
        String absent(String s);
    }

    public static abstract class FrontendImpl extends AbstractRulesFrontend {

        @Override
        public Object execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object... params) throws MethodInvocationException {
            return "Invoked: " + ruleName + " " + inputParamsTypes.length + " " + inputParamsTypes[0].getSimpleName();
        }
    }
}
