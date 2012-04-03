package org.openl.rules.ruleservice.publish.cache;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Statistics;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.ruleservice.simple.RulesFrontendImpl;

public class EhcacheTestModulesCacheTest {
    private static JavaClassRuleServicePublisher publisher;

    private static RulesFrontend frontend;

    private static RulesProjectResolver resolver;

    private static OpenLService service1;

    private static OpenLService service2;

    private Cache cache;

    private ModulesCache modulesCache;
    
    private static RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory;

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
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("multiModule", "no_url", null, false, modules1);
        Collection<Module> modules2 = resolveAllModules(new File("./test-resources/multi-module-2"));
        service2 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("multiModule2", "no_url", null, false, modules2);
    }

    @Before
    public void before() throws Exception {
        modulesCache = ModulesCache.getInstance();
        modulesCache.reset();
        Class<?> clazz = ModulesCache.class;
        Field field = clazz.getDeclaredField("cache");
        field.setAccessible(true);
        cache = (Cache) field.get(modulesCache);
        cache.setStatisticsAccuracy(Statistics.STATISTICS_ACCURACY_GUARANTEED);
        cache.setStatisticsEnabled(true);
        publisher.undeploy(service1.getName());
        publisher.undeploy(service2.getName());
        publisher.deploy(service1);
        publisher.deploy(service2);
    }

    // Correct usage ehcache in ModulesCache test
    @Test
    public void testModulesCache() throws Exception {
        assertEquals(0, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(0, cache.getStatistics().getCacheMisses());
        assertEquals(2, publisher.getServices().size());
        assertEquals(2, Array.getLength(frontend.getValue("multiModule", "data1")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(1, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValue("multiModule2", "data1")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(2, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValue("multiModule", "data1")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(3, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValue("multiModule2", "data1")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(0, cache.getStatistics().getCacheHits());
        assertEquals(4, cache.getStatistics().getCacheMisses());
        assertEquals(2, Array.getLength(frontend.getValue("multiModule2", "data1")));
        assertEquals(1, cache.getStatistics().getObjectCount());
        assertEquals(1, cache.getStatistics().getCacheHits());
        assertEquals(4, cache.getStatistics().getCacheMisses());
    }
}
