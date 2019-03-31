package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManager;
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

        assertEquals("World, Good Morning!", frontend.execute(MULTI_MODULE, "worldHello", new Object[] { 10 }));
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(3, Array.getLength(frontend.getValue(MULTI_MODULE, DATA2)));
    }

    public static interface SimpleInterface {
        String worldHello(int hour);
    }

    @Test
    public void testMultipleServices() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        RuleServicePublisher publisher = applicationContext.getBean("ruleServicePublisher", RuleServicePublisher.class);
        assertEquals(2, publisher.getServices().size());
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(2, Array.getLength(frontend.getValue(TUTORIAL4, COVERAGE)));
        publisher.undeploy(TUTORIAL4);
        assertNull(frontend.getValue(TUTORIAL4, COVERAGE));
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(1, publisher.getServices().size());
    }

    @Test
    public void testCompilationByRequest() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        RuleServicePublisher publisher = applicationContext.getBean("ruleServicePublisher", RuleServicePublisher.class);
        assertEquals(2, frontend.getServiceNames().size());
        Field openClassField = OpenLService.class.getDeclaredField("openClass");
        boolean accessible = openClassField.isAccessible();
        try {
            openClassField.setAccessible(true);
            for (OpenLService service : publisher.getServices()) {
                Object v = openClassField.get(service);
                assertNull("OpenLService must be not compiled for java publisher if not used before!", v);
            }
        } finally {
            openClassField.setAccessible(accessible);
        }
    }

    private int getCount(RuleServicePublisher publisher) throws Exception {
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
        RuleServicePublisher publisher = applicationContext.getBean("ruleServicePublisher", RuleServicePublisher.class);
        int count = getCount(publisher);
        final int executedTimes = 10;
        for (int i = 0; i < executedTimes; i++) {
            assertEquals(2, Array.getLength(frontend.getValue(TUTORIAL4, COVERAGE)));
        }
        int c = getCount(publisher);
        assertEquals(executedTimes, c - count);
        Object driver = publisher.getServiceByName(TUTORIAL4)
            .getServiceClass()
            .getClassLoader()
            .loadClass(DRIVER)
            .newInstance();
        frontend.execute(TUTORIAL4, "driverAgeType", new Object[] { driver });
    }

    @Test
    public void testMethodAfterInterceptors() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        RuleServicePublisher publisher = applicationContext.getBean("ruleServicePublisher", RuleServicePublisher.class);
        Object driver = publisher.getServiceByName(TUTORIAL4)
            .getServiceClass()
            .getClassLoader()
            .loadClass(DRIVER)
            .newInstance();
        Method nameSetter = driver.getClass().getMethod("setName", String.class);
        nameSetter.invoke(driver, "name");
        Class<? extends Object> returnType = frontend.execute(TUTORIAL4, "driverAgeType", new Object[] { driver })
            .getClass();
        assertTrue(returnType.isEnum());
        assertTrue(returnType.getName().equals("org.openl.rules.tutorial4.DriverAgeType"));
    }

    @Test
    public void testServiceClassResolving() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RuleServicePublisher publisher = applicationContext.getBean("ruleServicePublisher", RuleServicePublisher.class);

        Class<?> tutorial4ServiceClass = publisher.getServiceByName(TUTORIAL4).getServiceClass();
        assertTrue(tutorial4ServiceClass.isInterface());
        assertEquals(TUTORIAL4_INTERFACE, tutorial4ServiceClass.getName());

        Class<?> multiModuleServiceClass = publisher.getServiceByName(MULTI_MODULE).getServiceClass();
        Collection<Module> modules = publisher.getServiceByName(MULTI_MODULE).getModules();
        for (Module module : modules) {
            RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
            dependencyManager.setExecutionMode(true);
            IDependencyLoader loader = new RulesModuleDependencyLoader(modules);
            dependencyManager.setDependencyLoaders(Arrays.asList(loader));
            RulesInstantiationStrategy instantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(module, true, dependencyManager);
            Class<?> moduleServiceClass = instantiationStrategy.getInstanceClass();
            for (Method method : moduleServiceClass.getMethods()) {
                assertNotNull(MethodUtils.getMatchingAccessibleMethod(multiModuleServiceClass,
                    method.getName(),
                    method.getParameterTypes()));
            }
        }

    }
}
