package org.openl.rules.ui;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;

/** What a change of the design repository tells the screens following the project open. */
class WebStudioRepositoryModifiedTest {

    private final ProjectModel model = mock(ProjectModel.class);
    private final RulesProject project = mock(RulesProject.class);

    private WebStudio studio(boolean inPlace) {
        var studio = WebStudioMocks.studio(model);
        ReflectionTestUtils.setField(studio, "currentProject", new ProjectDescriptor());
        doReturn(project).when(studio).getCurrentProject();
        when(project.isOpened()).thenReturn(true);
        when(project.isInPlace()).thenReturn(inPlace);
        return studio;
    }

    @Test
    void tellsTheScreensOfEveryChangeOfTheFilesOfAProjectEditedInPlace() {
        var studio = studio(true);
        // A workbook written shows the change by its size and time before the event about it arrives
        when(model.isChangedOnDisk()).thenReturn(true);

        studio.onRepositoryModified();
        studio.onRepositoryModified();

        verify(model, times(2)).checkProjectFiles();
        verify(model, times(2)).publishStatusChanged();
    }

    @Test
    void tellsNothingWhileTheFilesStandAsCompiled() {
        var studio = studio(true);
        when(model.isChangedOnDisk()).thenReturn(false);

        studio.onRepositoryModified();

        verify(model).checkProjectFiles();
        verify(model, never()).publishStatusChanged();
    }

    @Test
    void readsNoFilesOfACopyInTheWorkspace() {
        var studio = studio(false);

        studio.onRepositoryModified();

        // The event is about the folder of the design repository, which a copy in the workspace is not in.
        verify(model, never()).checkProjectFiles();
        verify(model, never()).publishStatusChanged();
    }
}
