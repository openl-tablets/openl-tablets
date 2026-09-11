package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.compare.model.ComparisonStartedView;
import org.openl.studio.compare.service.ComparisonContent;
import org.openl.studio.compare.service.ComparisonLauncher;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
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
    private ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private ProjectsMergeConflictsService mergeConflictsService;
    private ProjectIdentifierMapper projectIdentifierMapper;
    private ProjectModel model;
    private ComparisonLauncher comparisonLauncher;
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
        conflictsSessionHolder = new ProjectsMergeConflictsSessionHolder();
        mergeConflictsService = mock(ProjectsMergeConflictsService.class);
        projectIdentifierMapper = mock(ProjectIdentifierMapper.class);

        comparisonLauncher = mock(ComparisonLauncher.class);

        controller = new ProjectsMergeController(mergeService,
                projectService,
                conflictsSessionHolder,
                mergeConflictsService,
                projectIdentifierMapper,
                comparisonLauncher);
    }

    @Test
    void comparesTheTwoVersionsOfAConflictedWorkbook() throws Exception {
        var conflictInfo = givenConflict();
        var theirs = fileItem("theirs");
        var ours = fileItem("ours");
        when(mergeConflictsService.getConflictFileItem(conflictInfo, "rules/Rates.xlsx", ConflictBase.THEIRS))
                .thenReturn(theirs);
        when(mergeConflictsService.getConflictFileItem(conflictInfo, "rules/Rates.xlsx", ConflictBase.OURS))
                .thenReturn(ours);
        when(comparisonLauncher.startContentOf(any())).thenReturn(new ComparisonStartedView("cmp-1"));

        var started = controller.compareConflictedFile(project, "rules/Rates.xlsx");

        assertEquals("cmp-1", started.id());
        // The version being merged in is read first, so the comparison reads as what the merge brings.
        var files = ArgumentCaptor.forClass(List.class);
        verify(comparisonLauncher).startContentOf(files.capture());
        assertEquals(List.of("Rates.xlsx", "Rates.xlsx"),
                ((List<ComparisonContent>) files.getValue()).stream().map(ComparisonContent::name).toList());
    }

    @Test
    void refusesToCompareAFileThatIsNotAWorkbook() {
        // A file of any other format is compared line by line, which the screen does itself.
        assertThrows(BadRequestException.class, () -> controller.compareConflictedFile(project, "rules.xml"));
    }

    @Test
    void releasesTheVersionItHasReadWhenTheOtherOneCannotBeRead() throws Exception {
        var conflictInfo = givenConflict();
        var theirs = fileItem("theirs");
        when(mergeConflictsService.getConflictFileItem(conflictInfo, "rules/Rates.xlsx", ConflictBase.THEIRS))
                .thenReturn(theirs);
        when(mergeConflictsService.getConflictFileItem(conflictInfo, "rules/Rates.xlsx", ConflictBase.OURS))
                .thenThrow(new IllegalStateException("no such revision"));

        assertThrows(IllegalStateException.class,
                () -> controller.compareConflictedFile(project, "rules/Rates.xlsx"));

        // The version that was read stays open otherwise, because nothing takes it over.
        verify(theirs.getStream()).close();
        verify(comparisonLauncher, never()).startContentOf(any());
    }

    /** A conflict of the project this session is holding. */
    private MergeConflictInfo givenConflict() {
        var projectId = ProjectIdModel.builder().repository("design").projectName("RateCalculator").build();
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);
        var conflictInfo = mock(MergeConflictInfo.class);
        conflictsSessionHolder.store(projectId, conflictInfo);
        return conflictInfo;
    }

    /** A version of a conflicted file, whose stream tells whether it was closed. */
    private static FileItem fileItem(String content) {
        return new FileItem(new FileData(),
                spy(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
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
