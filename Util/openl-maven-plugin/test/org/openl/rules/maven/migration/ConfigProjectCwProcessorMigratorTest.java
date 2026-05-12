package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

class ConfigProjectCwProcessorMigratorTest {

    @Test
    void dropsCwPropertyFileNameProcessor() {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setPropertiesFileNameProcessor(
                "org.openl.rules.project.resolving.CWPropertyFileNameProcessor");

        ConfigProjectCwProcessorMigrator.transform(descriptor);

        assertNull(descriptor.getPropertiesFileNameProcessor());
    }

    @Test
    void keepsCustomPropertyFileNameProcessor() {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setPropertiesFileNameProcessor("com.example.MyProcessor");

        ConfigProjectCwProcessorMigrator.transform(descriptor);

        assertEquals("com.example.MyProcessor", descriptor.getPropertiesFileNameProcessor());
    }

    @Test
    void leavesNullProcessorUntouched() {
        ProjectDescriptor descriptor = new ProjectDescriptor();

        ConfigProjectCwProcessorMigrator.transform(descriptor);

        assertNull(descriptor.getPropertiesFileNameProcessor());
    }
}
