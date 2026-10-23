package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;

/** How the studio finds the project of the workspace a descriptor was resolved for. */
class WebStudioProjectLookupTest {

    private final RulesProject project = mock(RulesProject.class);
    private final RulesProject another = mock(RulesProject.class);
    private final WebStudio studio = WebStudioMocks.studio(mock(ProjectModel.class));

    @Test
    void findsACopyInTheWorkspaceByTheFolderNamedAfterIt() {
        when(project.getFolderPath()).thenReturn("Rates");
        doReturn(project).when(studio).getProject("design", "Rates");

        assertSame(project, studio.projectOf("design", descriptor("Rate Tables", "/workspace/design/Rates")));
    }

    @Test
    void findsAProjectEditedInPlaceByTheNameItsDescriptorDeclares() {
        when(project.getFolderPath()).thenReturn("rates");
        doReturn(null).when(studio).getProject("design", "rates");
        doReturn(project).when(studio).getProject("design", "Rates");

        // The folder of the design repository is not named after the project
        assertSame(project, studio.projectOf("design", descriptor("Rates", "/repositories/design/rates")));
    }

    @Test
    void passesOverAnotherProjectNamedAsTheFolderOfTheDescriptor() {
        when(another.getFolderPath()).thenReturn("a/Rating");
        when(project.getFolderPath()).thenReturn("b/Rating");
        doReturn(another).when(studio).getProject("design", "Rating");
        doReturn(project).when(studio).getProject("design", "Rating B");

        assertSame(project, studio.projectOf("design", descriptor("Rating B", "/repositories/design/b/Rating")));
    }

    @Test
    void findsNoProjectTheWorkspaceDoesNotHold() {
        doReturn(null).when(studio).getProject("design", "rates");
        doReturn(null).when(studio).getProject("design", "Rates");

        assertNull(studio.projectOf("design", descriptor("Rates", "/repositories/design/rates")));
    }

    private static ProjectDescriptor descriptor(String name, String folder) {
        var descriptor = new ProjectDescriptor();
        descriptor.setName(name);
        descriptor.setProjectFolder(Path.of(folder));
        return descriptor;
    }
}
