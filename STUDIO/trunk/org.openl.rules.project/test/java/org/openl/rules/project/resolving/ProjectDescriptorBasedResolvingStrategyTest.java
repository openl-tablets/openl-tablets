package org.openl.rules.project.resolving;
import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Test;
import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectDescriptorBasedResolvingStrategyTest {

    @Test
    public void testResolving1 () {
        ProjectDescriptorBasedResolvingStrategy strategy = new ProjectDescriptorBasedResolvingStrategy();
        File folder = new File("test/resources/descriptor");
        assertTrue(strategy.isRulesProject(folder, null));
        
        ProjectDescriptor descriptor = strategy.resolveProject(folder, null);
        assertNotNull(descriptor);
        
        assertEquals(1, descriptor.getModules().size());
        assertEquals(1, descriptor.getClasspath().size());
    }
    
    @Test
    public void testResolving2 () {
        ProjectDescriptorBasedResolvingStrategy strategy = new ProjectDescriptorBasedResolvingStrategy();
        File folder = new File("test/resources");
        assertFalse(strategy.isRulesProject(folder, null));
    }

}
