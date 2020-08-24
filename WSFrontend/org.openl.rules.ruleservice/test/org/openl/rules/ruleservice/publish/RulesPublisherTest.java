package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.servlet.ServiceInfoProvider;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/RulesPublisherTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class RulesPublisherTest implements ApplicationContextAware {
    private static final String DRIVER = "org.openl.generated.beans.publisher.test.Driver";
    private static final String COVERAGE = "coverage";
    private static final String DATA2 = "data2";
    private static final String TUTORIAL4_INTERFACE = "org.openl.rules.tutorial4.Tutorial4Interface";
    private static final String DATA1 = "data1";
    private static final String TUTORIAL4 = "org.openl.rules.tutorial4.Tutorial4Interface";
    private static final String MULTI_MODULE = "RulesPublisherTest_multimodule";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testMultiModuleService() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);

        assertEquals("World, Good Morning!", frontend.execute(MULTI_MODULE, "worldHello", 10));
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(3, Array.getLength(frontend.getValue(MULTI_MODULE, DATA2)));
    }

    public interface SimpleInterface {
        String worldHello(int hour);
    }

    @Test
    public void testMultipleServices() throws Exception {
        assertNotNull(applicationContext);
        ServiceInfoProvider serviceManager = applicationContext.getBean("serviceManager", ServiceInfoProvider.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        ServiceManager publisher = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertEquals(2, serviceManager.getServicesInfo().size());
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(2, Array.getLength(frontend.getValue(TUTORIAL4, COVERAGE)));
        publisher.undeploy(TUTORIAL4);
        try {
            frontend.getValue(TUTORIAL4, COVERAGE);
            Assert.fail();
        } catch (MethodInvocationException ignored) {
        }
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
    }

    @Test
    public void testCompilationByRequest() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        Collection<String> serviceNames = frontend.getServiceNames();
        assertEquals(2, serviceNames.size());
        for (String sn : serviceNames) {
            OpenLService service = serviceManager.getServiceByName(sn);
            assertNull("OpenLService must be not compiled for java publisher if not used before.", service.getCompiledOpenClass());
        }
    }

    private int getCount(ServiceManager publisher) throws Exception {
        Class<?> counter = publisher.getServiceByName(TUTORIAL4)
            .getServiceBean()
            .getClass()
            .getClassLoader()
            .loadClass("org.openl.rules.tutorial4.InvocationCounter");
        return (Integer) counter.getMethod("getCount").invoke(null);
    }

    @Test(expected = MethodInvocationException.class)
    public void testMethodBeforeInterceptors() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        int count = getCount(serviceManager);
        final int executedTimes = 10;
        for (int i = 0; i < executedTimes; i++) {
            assertEquals(2, Array.getLength(frontend.getValue(TUTORIAL4, COVERAGE)));
        }
        int c = getCount(serviceManager);
        assertEquals(executedTimes, c - count);
        Object driver = serviceManager.getServiceByName(TUTORIAL4)
            .getServiceClass()
            .getClassLoader()
            .loadClass(DRIVER)
            .newInstance();
        frontend.execute(TUTORIAL4, "driverAgeType", driver);
    }

    @Test
    public void testMethodAfterInterceptors() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        Object driver = serviceManager.getServiceByName(TUTORIAL4)
            .getServiceClass()
            .getClassLoader()
            .loadClass(DRIVER)
            .newInstance();
        Method nameSetter = driver.getClass().getMethod("setName", String.class);
        nameSetter.invoke(driver, "name");
        Class<?> returnType = frontend.execute(TUTORIAL4, "driverAgeType", driver)
            .getClass();
        assertTrue(returnType.isEnum());
        assertEquals("org.openl.rules.tutorial4.DriverAgeType", returnType.getName());
    }

    @Test
    public void testServiceClassResolving() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        Class<?> tutorial4ServiceClass = serviceManager.getServiceByName(TUTORIAL4).getServiceClass();
        assertTrue(tutorial4ServiceClass.isInterface());
        assertEquals(TUTORIAL4_INTERFACE, tutorial4ServiceClass.getName());

        Class<?> multiModuleServiceClass = serviceManager.getServiceByName(MULTI_MODULE).getServiceClass();
        assertNotNull(multiModuleServiceClass);

    }
}
