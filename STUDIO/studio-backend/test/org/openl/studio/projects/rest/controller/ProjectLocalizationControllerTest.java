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
import org.openl.rules.repository.api.Page;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.model.SortOrder;
import org.openl.studio.projects.model.localization.LocalizationAction;
import org.openl.studio.projects.model.localization.LocalizationActions;
import org.openl.studio.projects.model.localization.LocalizationKeyView;
import org.openl.studio.projects.model.localization.LocalizationMessagesPage;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessagePositions;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.localization.LocalizationQuery;
import org.openl.studio.projects.service.localization.ProjectLocalizationService;

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
    void servesTheKeysAndThePagesAndMakesTheChangesThroughTheService() {
        var project = mock(RulesProject.class);
        var service = mock(ProjectLocalizationService.class);
        var keys = List.of(new LocalizationKeyView("greeting"));
        var query = new LocalizationQuery("hel", true, List.of("fr"), SortOrder.DESC);
        when(service.keys(project)).thenReturn(keys);
        var page = Page.of(1, 250);
        var answered = new LocalizationMessagesPage(List.of(), List.of(), page, 0);
        when(service.messages(project, query, "greeting", page)).thenReturn(answered);
        var controller = controller(mock(WorkspaceProjectService.class),
                mock(DetailedMessageDescriptionMapper.class),
                service);
        List<LocalizationAction> actions = List.of(new LocalizationAction.RemoveKey("greeting"));

        assertEquals(keys, controller.getKeys(project));
        assertEquals(answered,
                controller.getMessages(project, "hel", true, List.of("fr"), SortOrder.DESC,
                        "greeting", page));
        controller.applyActions(project, new LocalizationActions(actions));

        verify(service).apply(project, actions);
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
        return controller(workspace, mapper, mock(ProjectLocalizationService.class));
    }

    /** The controller checking, reading and changing the files through the services given. */
    private static ProjectLocalizationController controller(WorkspaceProjectService workspace,
                                                            DetailedMessageDescriptionMapper mapper,
                                                            ProjectLocalizationService service) {
        return new ProjectLocalizationController(workspace, mapper, service);
    }
}
