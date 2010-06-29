package org.openl.rules.project.resolving;

import static junit.framework.Assert.*;

import org.junit.Test;


public class RulesResolverTest {
    @Test
    public void testInitialization(){
        RulesProjectResolver resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        assertTrue(resolver.listOpenLProjects() != null);
    }
}
