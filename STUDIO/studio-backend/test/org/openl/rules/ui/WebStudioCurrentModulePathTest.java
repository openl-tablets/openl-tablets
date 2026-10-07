package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Verifies that {@link WebStudio#getCurrentModulePath} reports the open module's rules file
 * relative to the project root, as required by the project files REST API.
 *
 * @author Yury Molchan
 */
class WebStudioCurrentModulePathTest {

    private static WebStudio studio(Module module, ProjectDescriptor descriptor) {
        WebStudio studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        doReturn(module).when(studio).getCurrentModule();
        doReturn(descriptor).when(studio).getCurrentProjectDescriptor();
        return studio;
    }

    @Test
    void relativizesModuleFileAgainstProjectFolder() {
        var descriptor = new ProjectDescriptor();
        descriptor.setProjectFolder(Path.of("workspace", "Example Project"));
        var module = new Module();
        module.setProject(descriptor);
        module.setRulesRootPath("rules/Main Rules.xlsx");

        assertEquals("rules/Main Rules.xlsx", studio(module, descriptor).getCurrentModulePath());
    }

    @Test
    void emptyWhenNoModuleIsOpen() {
        assertEquals("", studio(null, null).getCurrentModulePath());
    }
}
