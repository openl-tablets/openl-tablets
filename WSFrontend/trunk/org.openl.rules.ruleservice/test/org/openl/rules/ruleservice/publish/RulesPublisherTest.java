package org.openl.rules.ruleservice.publish;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.MethodUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import org.openl.rules.ruleservice.core.ServiceDeployException;
import org.openl.rules.ruleservice.publish.cache.LazyMultiModuleInstantiationStrategy;
import org.openl.rules.ruleservice.simple.IRulesFrontend;
import org.openl.rules.ruleservice.simple.JavaClassDeploymentAdmin;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;

public class RulesPublisherTest {
    private static RulesPublisher publisher;
    private static IRulesFrontend frontend;
    private static RulesProjectResolver resolver;
    
    private static OpenLService service1;
    
    private static OpenLService service2;

    private static List<Module> resolveAllModules(File root) {
        List<Module> modules = new ArrayList<Module>();
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
    public static void init() throws ServiceDeployException{
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        frontend = new RulesFrontend();
        JavaClassDeploymentAdmin deploymentAdmin = new JavaClassDeploymentAdmin();
        deploymentAdmin.setFrontend(frontend);
        publisher = new RulesPublisher();
        publisher.setDeploymentAdmin(deploymentAdmin);
        publisher.setInstantiationFactory(new RulesInstantiationFactory());

        List<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module"));
        service1 = new OpenLService("multiModule", "no_url", modules1, null, false);
        File tut4Folder = new File("./test-resources/org.openl.tablets.tutorial4");
        ResolvingStrategy tut4ResolvingStrategy = resolver.isRulesProject(tut4Folder);
        assertNotNull(tut4ResolvingStrategy);
        service2 = new OpenLService("tutorial4", "no_url", tut4ResolvingStrategy
                .resolveProject(tut4Folder).getModules(), "org.openl.rules.tutorial4.Tutorial4Interface", false);
    }
    
    @Before
    public void before() throws ServiceDeployException {
        publisher.undeploy(service1.getName());
        publisher.undeploy(service2.getName());
        publisher.deploy(service1);
        publisher.deploy(service2);
    }

    //FIXME should be a test
    @Ignore
    @Test
    public void testMultiModuleService() throws MethodInvocationException{
        assertTrue(publisher.findServiceByName("multiModule").getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("World, Good Morning!", frontend.execute("multiModule", "worldHello", new Object[] { 10 }));
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(3, Array.getLength(frontend.getValues("multiModule", "data2")));
    }

    @Test
    public void testMultipleServices() throws Exception{
        assertEquals(2, publisher.getRunningServices().size());
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(2, Array.getLength(frontend.getValues("tutorial4", "coverage")));
        publisher.undeploy("tutorial4");
        assertNull(frontend.getValues("tutorial4", "coverage"));
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(1, publisher.getRunningServices().size());
    }
    
    private int getCount()throws Exception{
        Class<?> counter = publisher.findServiceByName("tutorial4").getServiceClass().getClassLoader()
                .loadClass("org.openl.rules.tutorial4.InvocationCounter");
        return (Integer)counter.getMethod("getCount").invoke(null);
    }
    
    @Test(expected=MethodInvocationException.class)
    public void testMethodBeforeInterceptors() throws Exception{
        int count = getCount();
        final int executedTimes = 10;
        for(int i = 0; i < executedTimes; i++){
            assertEquals(2, Array.getLength(frontend.getValues("tutorial4", "coverage")));
        }
        assertEquals(executedTimes, getCount() - count);
        Object driver = publisher.findServiceByName("tutorial4").getServiceClass().getClassLoader()
                .loadClass("org.openl.generated.beans.Driver").newInstance();
        System.out.println(frontend.execute("tutorial4", "driverAgeType", new Object[] { driver }));
    }

    @Test
    public void testMethodAfterInterceptors() throws Exception{
        Object driver = publisher.findServiceByName("tutorial4").getServiceClass().getClassLoader()
                .loadClass("org.openl.generated.beans.Driver").newInstance();
        Method nameSetter = driver.getClass().getMethod("setName", String.class);
        nameSetter.invoke(driver, "name");
        Class<? extends Object> returnType = frontend.execute("tutorial4", "driverAgeType", new Object[] { driver }).getClass();
        assertTrue(returnType.isEnum());
        assertTrue(returnType.getName().equals("org.openl.rules.tutorial4.DriverAgeType"));
    }

    @Test
    public void testServiceClassResolving() throws Exception{
        Class<?> tutorial4ServiceClass = publisher.findServiceByName("tutorial4").getServiceClass();
        assertTrue(tutorial4ServiceClass.isInterface());
        assertEquals("org.openl.rules.tutorial4.Tutorial4Interface", tutorial4ServiceClass.getName());

        Class<?> multiModuleServiceClass = publisher.findServiceByName("multiModule").getServiceClass();
        List<Module> modules = publisher.findServiceByName("multiModule").getModules();
        for (Module module : modules) {
            RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
            dependencyManager.setExecutionMode(true);
            IDependencyLoader loader = new RulesModuleDependencyLoader(modules);
            dependencyManager.setDependencyLoaders(Arrays.asList(loader));

            RulesInstantiationStrategy instantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(module,
                    dependencyManager);
            Class<?> moduleServiceClass = instantiationStrategy.getServiceClass();
            for (Method method : moduleServiceClass.getMethods()) {
                assertNotNull(MethodUtils.getMatchingAccessibleMethod(multiModuleServiceClass, method.getName(),
                        method.getParameterTypes()));
            }
        }

        assertTrue(tutorial4ServiceClass.isInterface());
        assertEquals("org.openl.rules.tutorial4.Tutorial4Interface", tutorial4ServiceClass.getName());
    }
}
