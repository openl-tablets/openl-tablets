package org.openl.rules.ruleservice.publish;

import static junit.framework.Assert.assertEquals;

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
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontendImpl;

public class MultiModuleDispatchingTest {
    private static final String SERVICE_NAME = "multiModule";
    private static JavaClassRuleServicePublisher publisher;
    private static RulesFrontend frontend;
    private static RulesProjectResolver resolver;
    private static RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory;

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
    public static void init() throws RuleServiceDeployException {
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        frontend = new RulesFrontendImpl();
        publisher = new JavaClassRuleServicePublisher();
        publisher.setFrontend(frontend);

        ruleServiceOpenLServiceInstantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
    }

    @Before
    public void before() throws Exception {
        publisher.undeploy(SERVICE_NAME);
    }

    @Test
    public void testMultiModuleService() throws Exception {
        List<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module_overloaded"));
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService(SERVICE_NAME, "no_url", null, true,
                modules1);

        publisher.deploy(service1);

        testDispatching();
    }

    /**
     * Test for module name patterns in project descriptor.
     */
    @Test
    public void testMultiModuleService2() throws Exception {
        ProjectDescriptor descriptor = resolveAllModulesUsingDescriptor(new File(
                "./test-resources/multi-module_overloaded"));
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService(SERVICE_NAME, "no_url", null, true,
                descriptor.getModules());
        publisher.deploy(service1);

        testDispatching();
    }

    public void testDispatching() throws MethodInvocationException {
        DefaultRulesRuntimeContext cxt = new DefaultRulesRuntimeContext();

        // dispatcher table
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_DT);
        cxt.setLob("lob1_1");
        //assertTrue(publisher.findServiceByName(SERVICE_NAME).getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("Hello1", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));

        // dispatching by java code
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_JAVA);
        cxt.setLob("lob1_1");
        //assertTrue(publisher.findServiceByName(SERVICE_NAME).getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("Hello1", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(SERVICE_NAME, "hello", new Object[] { cxt }));
    }
}
