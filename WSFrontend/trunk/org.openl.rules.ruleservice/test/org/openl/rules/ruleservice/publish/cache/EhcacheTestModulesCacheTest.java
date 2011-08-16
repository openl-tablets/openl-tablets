package org.openl.rules.ruleservice.publish.cache;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.ServiceDeployException;
import org.openl.rules.ruleservice.publish.RulesInstantiationFactory;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.simple.IRulesFrontend;
import org.openl.rules.ruleservice.simple.JavaClassDeploymentAdmin;
import org.openl.rules.ruleservice.simple.RulesFrontend;

public class EhcacheTestModulesCacheTest {
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
    
    //Correct usage ehcache in ModulesCache test
    @Test
    @Ignore
    public void testModulesCache() throws Exception{
        ModulesCache modulesCache = ModulesCache.getInstance();
        Class<?> clazz = ModulesCache.class;
        Field field = clazz.getDeclaredField("cache");
        field.setAccessible(true);
        Cache cache = (Cache)field.get(modulesCache);
        assertEquals(0, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(0, cache.getStatistics().getCacheMisses());
        assertEquals(2, publisher.getRunningServices().size());
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(1, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValues("tutorial4", "coverage")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(2, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValues("multiModule", "data1")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(3, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValues("tutorial4", "coverage")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(4, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValues("tutorial4", "coverage")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(1, cache.getStatistics().getCacheHits());
        assertEquals(4, cache.getStatistics().getCacheMisses());
    }
}    
