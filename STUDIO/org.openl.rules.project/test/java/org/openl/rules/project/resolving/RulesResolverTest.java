package org.openl.rules.project.resolving;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class RulesResolverTest {
    @Test
    public void testInitialization() {
        RulesProjectResolver resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        assertTrue(resolver.getResolvingStrategies().size() > 0);
        assertTrue(resolver.listOpenLProjects() != null);
    }

    @Test
    public void testStrategySelection() {
        RulesProjectResolver resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        assertTrue(resolver.isRulesProject(new File("test/resources/descriptor")) instanceof ProjectDescriptorBasedResolvingStrategy);
        assertTrue(resolver.isRulesProject(new File("test/resources/excel")) instanceof SimpleXlsResolvingStrategy);
    }
}
