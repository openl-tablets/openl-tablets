package org.openl.rules.ruleservice.multimodule.top;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.InitializingModuleListener;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.ruleservice.simple.RulesFrontendImpl;

public class TopMultiModuleDispatchingTest {
    private static final String SERVICE_NAME = "multiModule";
    private static JavaClassRuleServicePublisher publisher;
    private static RulesFrontend frontend;
    private static RulesProjectResolver resolver;
    private static RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory;

    private static OpenLService service1;

    private static ProjectDescriptor resolveAllModulesUsingDescriptor(File root) {
        ResolvingStrategy resolvingStrategy = resolver.isRulesProject(root);
        resolvingStrategy.setInitializingModuleListeners(Arrays.asList((InitializingModuleListener)new ModuleInitializingListener()));
        return resolvingStrategy.resolveProject(root);
    }

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
    public static void init() throws RuleServiceDeployException {
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        frontend = new RulesFrontendImpl();
        publisher = new JavaClassRuleServicePublisher();
        publisher.setFrontend(frontend);

        ruleServiceOpenLServiceInstantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        
        ruleServiceOpenLServiceInstantiationFactory.getInstantiationStrategyFactory();
    }

    @Before
    public void before() throws Exception {
        publisher.undeploy(SERVICE_NAME);
    }

 //   @Test
    public void testMultiModuleService() throws Exception {
        Collection<Module> modules1 = resolveAllModules(new File("./test-resources/top-multi-module"));
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
        		"./test-resources/top-multi-module"));
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService(SERVICE_NAME, "no_url", null, true,
                descriptor.getModules());
        
        publisher.deploy(service1);

        testDispatching();
    }

    public void testDispatching() throws MethodInvocationException {
        DefaultRulesRuntimeContext cxt = new DefaultRulesRuntimeContext();

        // dispatcher table
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_DT);
        cxt.setCurrentDate(new Date("01/01/2011"));
        //assertTrue(publisher.findServiceByName(SERVICE_NAME).getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("h11h21", frontend.execute(SERVICE_NAME, "hello1", new Object[] { cxt }));
        
        assertEquals("d11h21", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2011" }));
        assertEquals("d11h22", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2012" }));

        cxt.setCurrentDate(new Date("01/01/2012"));
        assertEquals("h21h22", frontend.execute(SERVICE_NAME, "hello1", new Object[] { cxt }));
        
        assertEquals("d21h21", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2011" }));
        assertEquals("d21h22", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2012" }));

        // dispatching by java code
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_JAVA);
        cxt.setCurrentDate(new Date("01/01/2011"));
        //assertTrue(publisher.findServiceByName(SERVICE_NAME).getInstantiationStrategy() instanceof LazyMultiModuleInstantiationStrategy);
        assertEquals("h11h21", frontend.execute(SERVICE_NAME, "hello1", new Object[] { cxt }));
        
        assertEquals("d11h21", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2011" }));
        assertEquals("d11h22", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2012" }));

        cxt.setCurrentDate(new Date("01/01/2012"));
        assertEquals("h21h22", frontend.execute(SERVICE_NAME, "hello1", new Object[] { cxt }));

        assertEquals("d21h21", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2011" }));
        assertEquals("d21h22", frontend.execute(SERVICE_NAME, "disp1", new Object[] { cxt, "01/01/2012" }));
    }
}
