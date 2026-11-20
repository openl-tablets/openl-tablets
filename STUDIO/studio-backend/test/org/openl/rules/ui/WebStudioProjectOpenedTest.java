package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Which project the model holds: a project of the same name in another folder is another one to compile.
 *
 * @author Yury Molchan
 */
class WebStudioProjectOpenedTest {

    @Test
    void holdsTheProjectOfTheNameInTheSameFolder() {
        var studio = studioHolding(project("Domain", "maven/domain-1.0.0-SNAPSHOT-100"));

        assertTrue(studio.isProjectOpened(project("Domain", "maven/domain-1.0.0-SNAPSHOT-100")));
    }

    @Test
    void aNewDownloadOfTheProjectIsAnotherProject() {
        var studio = studioHolding(project("Domain", "maven/domain-1.0.0-SNAPSHOT-100"));

        assertFalse(studio.isProjectOpened(project("Domain", "maven/domain-1.0.0-SNAPSHOT-200")),
                "a new snapshot is unpacked into a folder of its own");
    }

    @Test
    void holdsNoProjectOfAnotherNameOrNoneAtAll() {
        var studio = studioHolding(project("Domain", "maven/domain-1.0.0-100"));

        assertFalse(studio.isProjectOpened(project("Other", "maven/domain-1.0.0-100")));
        assertFalse(studio.isProjectOpened(null));
        assertFalse(WebStudioMocks.studio(mock(ProjectModel.class)).isProjectOpened(project("Domain", "x")));
    }

    private static WebStudio studioHolding(ProjectDescriptor project) {
        var module = new Module();
        module.setProject(project);
        var model = mock(ProjectModel.class);
        when(model.getModuleInfo()).thenReturn(module);
        return WebStudioMocks.studio(model);
    }

    private static ProjectDescriptor project(String name, String folder) {
        var project = new ProjectDescriptor();
        project.setName(name);
        project.setProjectFolder(Path.of(folder));
        return project;
    }
}
