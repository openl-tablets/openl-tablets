package org.openl.rules.project.resolving;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class ProjectResolverTest {

    @Test
    public void testStrategySelection() {
        ProjectResolver resolver = ProjectResolver.getInstance();
        assertInstanceOf(ProjectDescriptorBasedResolvingStrategy.class, resolver.isRulesProject(Path.of("test-resources/descriptor")));
        assertInstanceOf(SimpleXlsResolvingStrategy.class, resolver.isRulesProject(Path.of("test-resources/excel")));
    }
}
