package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

class RulesXmlMigrationsCwProcessorTest {

    @Test
    void dropsCwPropertyFileNameProcessor() {
        var descriptor = new ProjectDescriptor();
        descriptor.setPropertiesFileNameProcessor(
                "org.openl.rules.project.resolving.CWPropertyFileNameProcessor");

        RulesXmlMigrations.cwProcessor(descriptor);

        assertNull(descriptor.getPropertiesFileNameProcessor());
    }

    @Test
    void keepsCustomPropertyFileNameProcessor() {
        var descriptor = new ProjectDescriptor();
        descriptor.setPropertiesFileNameProcessor("com.example.MyProcessor");

        RulesXmlMigrations.cwProcessor(descriptor);

        assertEquals("com.example.MyProcessor", descriptor.getPropertiesFileNameProcessor());
    }

    @Test
    void leavesNullProcessorUntouched() {
        var descriptor = new ProjectDescriptor();

        RulesXmlMigrations.cwProcessor(descriptor);

        assertNull(descriptor.getPropertiesFileNameProcessor());
    }
}
