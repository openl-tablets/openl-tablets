package org.openl.rules.ruleservice.simple;

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

public class RulesFrontendTest {
    private RulesFrontend frontend;

    private static RulesProjectResolver resolver;

    private static RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory;

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
    public static void init() throws Exception {
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        ruleServiceOpenLServiceInstantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();

        List<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module"));
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("multiModule", "no_url", null, false,
                modules1);
        File tut4Folder = new File("./test-resources/org.openl.tablets.tutorial4");
        ResolvingStrategy tut4ResolvingStrategy = resolver.isRulesProject(tut4Folder);
        assertNotNull(tut4ResolvingStrategy);
        service2 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("tutorial4", "no_url",
                "org.openl.rules.tutorial4.Tutorial4Interface", false, tut4ResolvingStrategy.resolveProject(tut4Folder)
                        .getModules());
    }

    @Before
    public void before() {
        frontend = new RulesFrontendImpl();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetServices() {
        List<OpenLService> services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        frontend.registerService(service1);
        services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        frontend.registerService(service2);
        services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 2);
        services.add(service2);
    }

    @Test
    public void testRegisterService() {
        List<OpenLService> services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        frontend.registerService(service1);
        services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        frontend.registerService(service2);
        services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 2);
    }

    @Test
    public void testUnregisterService() {
        List<OpenLService> services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        frontend.registerService(service1);
        services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        frontend.unregisterService(service1.getName());
        services = frontend.getServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
    }
}
