package org.openl.ruleservice.simple;

import static junit.framework.Assert.*;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.ruleservice.OpenLService;
import org.openl.ruleservice.ServiceDeployException;
import org.openl.ruleservice.publish.RulesInstantiationFactory;
import org.openl.ruleservice.publish.RulesPublisher;

public class SimpleFrontendTest {
    private RulesPublisher publisher;
    private RulesFrontend frontend;
    private RulesProjectResolver resolver;

    @Before
    public void init() throws ServiceDeployException {
        if (frontend == null) {
            resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
            frontend = new RulesFrontendImpl();
            JavaClassDeploymentAdmin deploymentAdmin = new JavaClassDeploymentAdmin();
            deploymentAdmin.setFrontend(frontend);
            publisher = new RulesPublisher();
            publisher.setDeploymentAdmin(deploymentAdmin);
            publisher.setInstantiationFactory(new RulesInstantiationFactory());

            List<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module"));
            OpenLService service1 = new OpenLService("multiModule", "no_url", modules1, null, false);
            publisher.deploy(service1);
            File tut4Folder = new File("./test-resources/org.openl.tablets.tutorial4");
            ResolvingStrategy tut4ResolvingStrategy = resolver.isRulesProject(tut4Folder);
            assertNotNull(tut4ResolvingStrategy);
            OpenLService service2 = new OpenLService("tutorial4", "no_url", tut4ResolvingStrategy.resolveProject(
                    tut4Folder).getModules(), "org.openl.rules.tutorial4.Tutorial4Interface", false);
            publisher.deploy(service2);
        }
    }

    private List<Module> resolveAllModules(File root) {
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

    @Test
    public void testMultiModuleService() throws Exception {
        assertTrue(publisher.findServiceByName("multiModule").getInstantiationStrategy() instanceof MultiModuleInstantiationStrategy);
        assertEquals("World, Good Morning!", frontend.execute("multiModule", "worldHello", new Object[] { 10 }));
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(3, Array.getLength(frontend.getValues("multiModule", "data2")));
    }

    @Test
    public void testMultipleServices() throws Exception {
        assertEquals(2, publisher.getRunningServices().size());
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(2, Array.getLength(frontend.getValues("tutorial4", "coverage")));
        publisher.undeploy("tutorial4");
        assertNull(frontend.getValues("tutorial4", "coverage"));
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(1, publisher.getRunningServices().size());
    }

    @Test
    public void testServiceClassResolving() throws Exception {
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
                assertNotNull(MethodUtils.getMatchingAccessibleMethod(moduleServiceClass, method.getName(),
                        method.getParameterTypes()));
            }
        }

        assertTrue(tutorial4ServiceClass.isInterface());
        assertEquals("org.openl.rules.tutorial4.Tutorial4Interface", tutorial4ServiceClass.getName());
    }
}
