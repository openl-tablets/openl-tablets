package org.openl.rules.project.resolving;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class ProjectResolverTest {

    @Test
    public void testStrategySelection() {
        ProjectResolver resolver = ProjectResolver.getInstance();
        assertTrue(resolver
            .isRulesProject(new File("test-resources/descriptor")) instanceof ProjectDescriptorBasedResolvingStrategy);
        assertTrue(resolver.isRulesProject(new File("test-resources/excel")) instanceof SimpleXlsResolvingStrategy);
    }
}
