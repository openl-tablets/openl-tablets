package org.openl.rules.ruleservice.publish.cache;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.RulesFrontendImpl;

@Ignore
public class EhcacheMemorySizeTest {
    private static final String ELEMENTS_RESTRICTION_CACHE = "elementsRestrictionCache";
    private static final String MEMORY_RESTRICTION_CACHE = "memoryRestrictionCache";
    
    private static JavaClassRuleServicePublisher publisher;

    private static RulesProjectResolver resolver;

    private static OpenLService service1;

    private static OpenLService service2;
    
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
        publisher = new JavaClassRuleServicePublisher();
        publisher.setFrontend(new RulesFrontendImpl());
                
        RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        
        Collection<Module> modules1 = resolveAllModules(new File("./test-resources/multi-module"));
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("multiModule", "no_url", null, false, modules1);
        Collection<Module> modules2 = resolveAllModules(new File("./test-resources/multi-module-2"));
        service2 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("multiModule2", "no_url", null, false, modules2);
    }

    @Before
    public void before() throws Exception {
        publisher.undeploy(service1.getName());
        publisher.undeploy(service2.getName());
        publisher.deploy(service1);
        publisher.deploy(service2);
    }

    
    @Test
    public void testElementsCount() throws Exception {
        Cache cache = CacheManager.create().getCache(ELEMENTS_RESTRICTION_CACHE);
        cache.getCacheConfiguration().setMaxEntriesLocalHeap(2);

        assertEquals(0, cache.getStatistics().getObjectCount());
        cache.put(new Element("1", service1));
        assertEquals(1, cache.getStatistics().getObjectCount());
        cache.put(new Element("2", service2));
        assertEquals(2, cache.getStatistics().getObjectCount());
        
        cache.removeAll();
        cache.clearStatistics();
        cache.getCacheConfiguration().setMaxEntriesLocalHeap(1);

        assertEquals(0, cache.getStatistics().getObjectCount());
        cache.put(new Element("1", service1));
        assertEquals(1, cache.getStatistics().getObjectCount());
        cache.put(new Element("2", service2));
        assertEquals(1, cache.getStatistics().getObjectCount());
    }
    
    @Test
    public void testMemorySize() throws Exception {
        Cache cache = CacheManager.create().getCache(MEMORY_RESTRICTION_CACHE);
        cache.getCacheConfiguration().setMaxBytesLocalHeap("3M");

        assertEquals(0, cache.getStatistics().getObjectCount());
        cache.put(new Element("1", service1));
        assertEquals(1, cache.getStatistics().getObjectCount());
        cache.put(new Element("2", service2));
        assertEquals(2, cache.getStatistics().getObjectCount());
        
        cache.removeAll();
        cache.clearStatistics();
        cache.getCacheConfiguration().setMaxBytesLocalHeap("1700k");

        assertEquals(0, cache.getStatistics().getObjectCount());
        cache.put(new Element("1", service1));
        assertEquals(1, cache.getStatistics().getObjectCount());
        cache.put(new Element("2", service2));
        assertEquals(1, cache.getStatistics().getObjectCount());
    }
}
