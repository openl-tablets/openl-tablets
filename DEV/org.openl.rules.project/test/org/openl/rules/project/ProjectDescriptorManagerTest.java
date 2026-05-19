package org.openl.rules.project;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

class ProjectDescriptorManagerTest {

    private static final Path DESCRIPTOR_ZIP = Path.of("test-resources/descriptor.zip");

    @Test
    void testIsCoveredByWildcardModule() throws Exception {
        assertIsCoveredByWildcardModule(ProjectDescriptor.read(Path.of("test-resources/descriptor/rules-wildcard.xml")));
    }

    @Test
    void zipArchive_testIsCoveredByWildcardModule() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            assertIsCoveredByWildcardModule(ProjectDescriptor.read(fs.getPath("/rules-wildcard.xml")));
        }
    }

    private void assertIsCoveredByWildcardModule(ProjectDescriptor descriptor) {
        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        Module newModule = new Module();
        newModule.setName("New Module");
        newModule.setRulesRootPath("rules/New Module.xlsx");
        assertTrue(manager.isCoveredByWildcardModule(descriptor, newModule));

        newModule.setRulesRootPath("rules\\New Module.xlsx");
        assertTrue(manager.isCoveredByWildcardModule(descriptor, newModule));

        newModule.setRulesRootPath("New Module.xlsx");
        assertFalse(manager.isCoveredByWildcardModule(descriptor, newModule));
    }

    private static FileSystem openZipFile(Path path) throws IOException {
        return FileSystems.newFileSystem(path, Thread.currentThread().getContextClassLoader());
    }
}
