package org.openl.rules.project.resolving;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class ProjectResolverTest {

    @Test
    public void testStrategySelection() {
        ProjectResolver resolver = ProjectResolver.instance();
        assertTrue(resolver.isRulesProject(new File("test-resources/descriptor")) instanceof ProjectDescriptorBasedResolvingStrategy);
        assertTrue(resolver.isRulesProject(new File("test-resources/excel")) instanceof SimpleXlsResolvingStrategy);
    }
}
