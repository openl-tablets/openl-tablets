package org.openl.rules.ruleservice.simple;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

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
import org.openl.rules.ruleservice.core.ServiceDeployException;

public class JavaClassDeploymentAdminTest {
    private static RulesProjectResolver resolver;

    private static OpenLService service1;

    private static OpenLService service2;

    private IRulesFrontend frontend;

    private JavaClassDeploymentAdmin javaClassDeploymentAdmin;

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
        List<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module"));
        service1 = new OpenLService("multiModule", "no_url", modules1, null, false);
        File tut4Folder = new File("./test-resources/org.openl.tablets.tutorial4");
        ResolvingStrategy tut4ResolvingStrategy = resolver.isRulesProject(tut4Folder);
        assertNotNull(tut4ResolvingStrategy);
        service2 = new OpenLService("tutorial4", "no_url", tut4ResolvingStrategy.resolveProject(tut4Folder)
                .getModules(), "org.openl.rules.tutorial4.Tutorial4Interface", false);
    }

    @Before
    public void before() {
        frontend = new RulesFrontend();
        javaClassDeploymentAdmin = new JavaClassDeploymentAdmin();
        javaClassDeploymentAdmin.setFrontend(frontend);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getRunningServices() throws ServiceDeployException{
        List<OpenLService> services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassDeploymentAdmin.deploy(service1);
        services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        javaClassDeploymentAdmin.deploy(service2);
        services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 2);
        services.add(service2);
    }

    @Test
    public void testDeploy() throws ServiceDeployException{
        List<OpenLService> services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassDeploymentAdmin.deploy(service1);
        services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        javaClassDeploymentAdmin.deploy(service2);
        services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 2);
    }

    @Test
    public void testUndeploy() throws ServiceDeployException{
        List<OpenLService> services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassDeploymentAdmin.deploy(service1);
        services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        javaClassDeploymentAdmin.undeploy(service1.getName());
        services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
    }
    
    @Test
    public void testFindServiceByName() throws ServiceDeployException{
        List<OpenLService> services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 0);
        javaClassDeploymentAdmin.deploy(service1);
        services = javaClassDeploymentAdmin.getRunningServices();
        assertNotNull(services);
        assertTrue(services.size() == 1);
        OpenLService service = javaClassDeploymentAdmin.findServiceByName(service1.getName());
        assertNotNull(service);
        assertEquals(service1.getName(), service.getName());
    }

}
