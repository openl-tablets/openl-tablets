package org.openl.rules.project.instantiation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class ModulePathSourceCodeModuleTest {

    @Test
    public void testUri() {
        final Path pathToProject = Paths.get("test/rules/test xls").toAbsolutePath();
        final Path pathToModule = Paths.get("test/rules/test xls/Test with spaces.xls").toAbsolutePath();
        Module module = new Module();
        module.setRulesRootPath(new PathEntry());
        module.getRulesRootPath().setPath(pathToModule.toString());

        module.setProject(mock(ProjectDescriptor.class));
        when(module.getProject().getProjectFolder()).thenReturn(pathToProject);

        assertEquals("test%20xls/Test%20with%20spaces.xls", module.getRelativeUri());

        ModulePathSourceCodeModule src = new ModulePathSourceCodeModule(module);

        final String actualFullUri = src.getFileUri();
        final String actualRelativeUri = src.getUri();
        assertTrue(actualFullUri.endsWith(actualRelativeUri));
        assertEquals("test%20xls/Test%20with%20spaces.xls", actualRelativeUri);
    }

}
