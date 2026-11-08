package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.message.OpenLFileMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessagePositions;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;

class ProjectLocalizationControllerTest {

    @Test
    void checksTheFilesOfAnOpenedProjectWithoutCompilingIt() {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(true);
        var descriptor = new ProjectDescriptor();
        var folder = Path.of("projects", "Rating");
        descriptor.setProjectFolder(folder);
        List<OpenLMessage> found = List.of(new OpenLFileMessage("Message 'a' is written twice.",
                folder.resolve("i18n/message.properties"), 2, "a", ""));
        var workspace = mock(WorkspaceProjectService.class);
        when(workspace.getProjectDescriptor(project)).thenReturn(descriptor);
        when(workspace.checkLocalization(descriptor)).thenReturn(found);
        var mapper = mock(DetailedMessageDescriptionMapper.class);
        var mapped = List.of(mock(DetailedMessageDescription.class));
        when(mapper.mapFileMessages(found, folder, MessagePositions.TEXT)).thenReturn(mapped);

        var problems = controller(workspace, mapper).getProblems(project, MessagePositions.TEXT);

        assertEquals(mapped, problems);
        // Nothing compiles: the files are read and checked, and the paths are told from the project folder.
        verify(workspace, never()).compileModule(any(), any(), any(Boolean.class));
    }

    @Test
    void refusesAProjectNobodyOpenedRatherThanTellingItHasNoProblems() {
        var project = mock(RulesProject.class);
        var workspace = mock(WorkspaceProjectService.class);
        when(workspace.getProjectDescriptor(project)).thenThrow(new ConflictException("project.not.opened.message"));
        var mapper = mock(DetailedMessageDescriptionMapper.class);
        var controller = controller(workspace, mapper);

        assertThrows(ConflictException.class, () -> controller.getProblems(project, MessagePositions.FILE));

        verify(workspace, never()).checkLocalization(any());
        verifyNoInteractions(mapper);
    }

    /** The controller checking the files through the services given. */
    private static ProjectLocalizationController controller(WorkspaceProjectService workspace,
                                                            DetailedMessageDescriptionMapper mapper) {
        return new ProjectLocalizationController(workspace, mapper);
    }
}
