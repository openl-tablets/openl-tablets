package org.openl.rules.ruleservice.simple;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;

public class JavaClassDeploymentAdminTest {
    private static RulesProjectResolver resolver;

    private static OpenLService service1;

    private static OpenLService service2;

    private RulesFrontend frontend;

    private JavaClassRuleServicePublisher javaClassRuleServicePublisher;
    
    private static RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory;

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
    public static void init() throws Exception {
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        ruleServiceOpenLServiceInstantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        
        List<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module"));
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("multiModule", "no_url", null, false, modules1);
        File tut4Folder = new File("./test-resources/org.openl.tablets.tutorial4");
        ResolvingStrategy tut4ResolvingStrategy = resolver.isRulesProject(tut4Folder);
        assertNotNull(tut4ResolvingStrategy);
        service2 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("tutorial4", "no_url", "org.openl.rules.tutorial4.Tutorial4Interface", false, tut4ResolvingStrategy.resolveProject(tut4Folder)
                        .getModules());
    }

    @Before
    public void before() {
        frontend = new RulesFrontendImpl();
        javaClassRuleServicePublisher = new JavaClassRuleServicePublisher();
        javaClassRuleServicePublisher.setFrontend(frontend);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getRunningServices() throws Exception {
        List<OpenLService> services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassRuleServicePublisher.deploy(service1);
        services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        javaClassRuleServicePublisher.deploy(service2);
        services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 2);
        services.add(service2);
    }

    @Test
    public void testDeploy() throws Exception {
        List<OpenLService> services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassRuleServicePublisher.deploy(service1);
        services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        javaClassRuleServicePublisher.deploy(service2);
        services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 2);
    }

    @Test
    public void testUndeploy() throws Exception {
        List<OpenLService> services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassRuleServicePublisher.deploy(service1);
        services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        javaClassRuleServicePublisher.undeploy(service1.getName());
        services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
    }

    @Test
    public void testFindServiceByName() throws Exception {
        List<OpenLService> services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassRuleServicePublisher.deploy(service1);
        services = javaClassRuleServicePublisher.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        OpenLService service = javaClassRuleServicePublisher.findServiceByName(service1.getName());
        assertNotNull(service);
        assertEquals(service1.getName(), service.getName());
    }

}
