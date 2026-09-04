package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeRequest;
import org.openl.studio.projects.model.merge.MergeResult;
import org.openl.studio.projects.model.merge.MergeResultStatus;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.merge.ProjectsMergeService;
import org.openl.studio.projects.service.project.compile.ProjectHandle;

/**
 * A merge happens in the repository, so a project that is not opened merges just like an opened one. The
 * editor is touched only when there is an editor to touch — see EPBDS-16420.
 */
class ProjectsMergeControllerTest {

    private static final MergeRequest REQUEST = new MergeRequest(MergeOpMode.SEND, "master");

    private RulesProject project;
    private WorkspaceProjectService projectService;
    private ProjectsMergeService mergeService;
    private ProjectModel model;
    private ProjectsMergeController controller;

    @BeforeEach
    void init() throws Exception {
        var repository = mock(BranchRepository.class);
        lenient().when(repository.getId()).thenReturn("design");

        project = mock(RulesProject.class);
        lenient().when(project.getName()).thenReturn("RateCalculator");
        lenient().when(project.getBranch()).thenReturn("EPBDS-16411-demo");
        lenient().when(project.getRealPath()).thenReturn("DESIGN/rules/RateCalculator");
        lenient().when(project.getDesignRepository()).thenReturn(repository);

        model = mock(ProjectModel.class);
        var handle = mock(ProjectHandle.class);
        lenient().when(handle.awaitCompiled()).thenReturn(model);

        projectService = mock(WorkspaceProjectService.class);
        lenient().when(projectService.openProject(project)).thenReturn(handle);
        lenient().when(projectService.getWebStudio()).thenReturn(mock(WebStudio.class));
        lenient().when(projectService.getUserWorkspace()).thenReturn(mock(UserWorkspace.class));

        mergeService = mock(ProjectsMergeService.class);
        lenient().when(mergeService.merge(any(), any(), any(), anyBoolean()))
                .thenReturn(MergeResult.builder().build());

        controller = new ProjectsMergeController(mergeService,
                projectService,
                new ProjectsMergeConflictsSessionHolder(),
                mock(ProjectsMergeConflictsService.class),
                mock(ProjectIdentifierMapper.class));
    }

    @Test
    void mergesAClosedProjectWithoutOpeningIt() throws Exception {
        when(project.isOpened()).thenReturn(false);

        var response = controller.merge(project, REQUEST, false);

        assertEquals(MergeResultStatus.SUCCESS, response.status());
        verify(mergeService).merge(project, "master", MergeOpMode.SEND, false);
        // Opening it would compile a project nobody is looking at — and used to fail with "not opened".
        verify(projectService, never()).openProject(project);
        verify(project, never()).open();
        verify(project, never()).close();
    }

    @Test
    void reopensAnOpenedProjectSoTheEditorShowsTheMergedContent() throws Exception {
        when(project.isOpened()).thenReturn(true);

        var response = controller.merge(project, REQUEST, false);

        assertEquals(MergeResultStatus.SUCCESS, response.status());
        verify(projectService).openProject(project);
        verify(model).clearModuleInfo();
        verify(project).close();
    }
}
