package org.openl.rules.ruleservice.publish;

import static junit.framework.Assert.*;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.MethodUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.publish.cache.LazyMultiModuleInstantiationStrategy;
import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.ruleservice.simple.RulesFrontendImpl;

public class RulesPublisherTest {
    private static final String COVERAGE = "coverage";
    private static final String DATA2 = "data2";
    private static final String NO_URL = "no_url";
    private static final String TUTORIAL4_INTERFACE = "org.openl.rules.tutorial4.Tutorial4Interface";
    private static final String DATA1 = "data1";
    private static final String TUTORIAL4 = "tutorial4";
    private static final String MULTI_MODULE = "multiModule";
    
    private static JavaClassRuleServicePublisher publisher;
    private static RulesFrontend frontend;
    private static RulesProjectResolver resolver;
    private static RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory;

    private static OpenLService service1;

    private static OpenLService service2;

    private static Collection<Module> resolveAllModules(File root) {
        Collection<Module> modules = new ArrayList<Module>();
        resolver.setWorkspace(root.getAbsolutePath());
        List<ProjectDescriptor> projects = resolver.listOpenLProjects();
        for (ProjectDescriptor project : projects) {
            for (Module module : project.getModules()) {
                modules.add(module);
            }
        }
        return modules;
    }

    @BeforeClass
    public static void init() throws Exception {
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        frontend = new RulesFrontendImpl();
        publisher = new JavaClassRuleServicePublisher();
        publisher.setFrontend(frontend);
        ruleServiceOpenLServiceInstantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();

        Collection<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module"));
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService(MULTI_MODULE, NO_URL, null, false,
                modules1);
        File tut4Folder = new File("./test-resources/org.openl.tablets.tutorial4");
        ResolvingStrategy tut4ResolvingStrategy = resolver.isRulesProject(tut4Folder);
        assertNotNull(tut4ResolvingStrategy);
        service2 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService(TUTORIAL4, NO_URL,
                TUTORIAL4_INTERFACE, false, tut4ResolvingStrategy.resolveProject(tut4Folder)
                        .getModules());
    }

    @Before
    public void before() throws Exception {
        publisher.undeploy(service1.getName());
        publisher.undeploy(service2.getName());
        publisher.deploy(service1);
        publisher.deploy(service2);
    }

    @Test
    public void testMultiModuleService() throws Exception {
        //assertTrue(publisher.findServiceByName("multiModule").getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("World, Good Morning!", frontend.execute(MULTI_MODULE, "worldHello", new Object[] { 10 }));
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(3, Array.getLength(frontend.getValue(MULTI_MODULE, DATA2)));
    }

    public static interface SimpleInterface{
    	String worldHello(int hour);
    }

    @Test
    public void testMultiModuleServiceClass() throws Exception {
        Collection<Module> modules = resolveAllModules(new File("./test-resources/multi-module"));
        LazyMultiModuleInstantiationStrategy instantiationStrategy = new LazyMultiModuleInstantiationStrategy(modules, null);
        instantiationStrategy.setServiceClass(SimpleInterface.class);
        Object instance = instantiationStrategy.instantiate();
        assertNotNull(instance);
        assertTrue(instance instanceof SimpleInterface);
    }

    @Test
    public void testMultipleServices() throws Exception {
        assertEquals(2, publisher.getServices().size());
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(2, Array.getLength(frontend.getValue(TUTORIAL4, COVERAGE)));
        publisher.undeploy(TUTORIAL4);
        assertNull(frontend.getValue(TUTORIAL4, COVERAGE));
        assertEquals(2, Array.getLength(frontend.getValue(MULTI_MODULE, DATA1)));
        assertEquals(1, publisher.getServices().size());
    }

    private int getCount() throws Exception {
        Class<?> counter = publisher.getServiceByName(TUTORIAL4).getServiceClass().getClassLoader()
                .loadClass("org.openl.rules.tutorial4.InvocationCounter");
        return (Integer) counter.getMethod("getCount").invoke(null);
    }

    @Test(expected = MethodInvocationException.class)
    public void testMethodBeforeInterceptors() throws Exception {
        int count = getCount();
        final int executedTimes = 10;
        for (int i = 0; i < executedTimes; i++) {
            assertEquals(2, Array.getLength(frontend.getValue(TUTORIAL4, COVERAGE)));
        }
        assertEquals(executedTimes, getCount() - count);
        Object driver = publisher.getServiceByName(TUTORIAL4).getServiceClass().getClassLoader()
                .loadClass("org.openl.generated.beans.Driver").newInstance();
        System.out.println(frontend.execute(TUTORIAL4, "driverAgeType", new Object[] { driver }));
    }

    @Test
    public void testMethodAfterInterceptors() throws Exception {
        Object driver = publisher.getServiceByName(TUTORIAL4).getServiceClass().getClassLoader()
                .loadClass("org.openl.generated.beans.Driver").newInstance();
        Method nameSetter = driver.getClass().getMethod("setName", String.class);
        nameSetter.invoke(driver, "name");
        Class<? extends Object> returnType = frontend.execute(TUTORIAL4, "driverAgeType", new Object[] { driver })
                .getClass();
        assertTrue(returnType.isEnum());
        assertTrue(returnType.getName().equals("org.openl.rules.tutorial4.DriverAgeType"));
    }

    @Test
    public void testServiceClassResolving() throws Exception {
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

            RulesInstantiationStrategy instantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(module,
                    dependencyManager);
            Class<?> moduleServiceClass = instantiationStrategy.getInstanceClass();
            for (Method method : moduleServiceClass.getMethods()) {
                assertNotNull(MethodUtils.getMatchingAccessibleMethod(multiModuleServiceClass, method.getName(),
                        method.getParameterTypes()));
            }
        }

        assertTrue(tutorial4ServiceClass.isInterface());
        assertEquals(TUTORIAL4_INTERFACE, tutorial4ServiceClass.getName());
    }
}
