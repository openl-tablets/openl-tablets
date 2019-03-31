package org.openl.rules.ruleservice.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    }

    @Test
    public void testOverridedFrontend() {
        assertNotNull(applicationContext);
        ServiceInterface service = applicationContext.getBean("ruleService2", ServiceInterface.class);
        assertNotNull(service);
        assertEquals("FrontendImpl_2 invoked", service.worldHello(10));
    }

    public interface ServiceInterface {
        String worldHello(int arg);
    }

    public static abstract class FrontendImpl extends AbstractRulesFrontend {

        @Override
        public Object execute(String serviceName, String ruleName, Object... params) throws MethodInvocationException {
            return "FrontendImpl_2 invoked";
        }
    }
}
