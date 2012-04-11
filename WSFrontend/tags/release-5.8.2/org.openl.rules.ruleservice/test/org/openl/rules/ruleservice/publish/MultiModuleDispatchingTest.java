package org.openl.rules.ruleservice.publish;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.context.DefaultRulesRuntimeContext;
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
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;

public class MultiModuleDispatchingTest {
    private static final String SERVICE_NAME = "multiModule";
    private static RulesPublisher publisher;
    private static IRulesFrontend frontend;
    private static RulesProjectResolver resolver;

    private static OpenLService service1;

    private static ProjectDescriptor resolveAllModulesUsingDescriptor(File root) {
        ResolvingStrategy rulesProject = resolver.isRulesProject(root);
        return rulesProject.resolveProject(root);
    }

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
    public static void init() throws ServiceDeployException {
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        frontend = new RulesFrontend();
        JavaClassDeploymentAdmin deploymentAdmin = new JavaClassDeploymentAdmin();
        deploymentAdmin.setFrontend(frontend);
        publisher = new RulesPublisher();
        publisher.setDeploymentAdmin(deploymentAdmin);
        publisher.setInstantiationFactory(new RulesInstantiationFactory());
    }

    @Before
    public void before() throws ServiceDeployException {
        publisher.undeploy(SERVICE_NAME);
    }

    @Test
    public void testMultiModuleService() throws Exception {
        List<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module_overloaded"));
        service1 = new OpenLService(SERVICE_NAME, "no_url", modules1, null, true);
        publisher.deploy(service1);

        testDispatching();
    }

    /**
     * Test for module name patterns in project descriptor. 
     */
    @Test
    public void testMultiModuleService2() throws Exception {
        ProjectDescriptor descriptor = resolveAllModulesUsingDescriptor(new File("./test-resources/multi-module_overloaded"));
        service1 = new OpenLService(SERVICE_NAME, "no_url", descriptor.getModules(), null, true);
        publisher.deploy(service1);

        testDispatching();
    }

    public void testDispatching() throws MethodInvocationException {
        DefaultRulesRuntimeContext cxt = new DefaultRulesRuntimeContext();

        // dispatcher table
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY,
            OpenLSystemProperties.DISPATCHING_MODE_DT);
        cxt.setLob("lob1_1");
        assertTrue(publisher.findServiceByName(SERVICE_NAME).getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("Hello1", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));

        // dispatching by java code
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY,
            OpenLSystemProperties.DISPATCHING_MODE_JAVA);
        cxt.setLob("lob1_1");
        assertTrue(publisher.findServiceByName(SERVICE_NAME).getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("Hello1", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
    }
}
