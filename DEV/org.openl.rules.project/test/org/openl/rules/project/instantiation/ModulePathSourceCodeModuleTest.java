package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

class ModulePathSourceCodeModuleTest {

    @Test
    void testUri() {
        final var pathToProject = Path.of("test/rules/test xls").toAbsolutePath();
        final var pathToModule = Path.of("test/rules/test xls/Test with spaces.xls").toAbsolutePath();
        var module = new Module();
        module.setRulesRootPath(pathToModule.toString());

        module.setProject(mock(ProjectDescriptor.class));
        when(module.getProject().getProjectFolder()).thenReturn(pathToProject);

        assertEquals("test%20xls/Test%20with%20spaces.xls", module.getRelativeUri());

        var src = new ModulePathSourceCodeModule(module);

        final var actualFullUri = src.getFileUri();
        final var actualRelativeUri = src.getUri();
        assertTrue(actualFullUri.endsWith(actualRelativeUri));
        assertEquals("test%20xls/Test%20with%20spaces.xls", actualRelativeUri);
    }

}
