package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.git.MergeConflictDetails;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeSummaryView;
import org.openl.studio.projects.model.merge.MergedChanges;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;

class ProjectsMergeConflictsServiceImplTest {

    private static final String FIRST_FILE = "rules/DeletedOurs.xlsx";
    private static final String SECOND_FILE = "rules/DeletedTheirs.xlsx";

    @Test
    void reportsAvailabilityForEveryConflictedFile() throws Exception {
        var context = createContext();
        var repository = context.repository();
        when(repository.checkHistory(SECOND_FILE, "ours")).thenReturn(file(SECOND_FILE));
        when(repository.checkHistory(FIRST_FILE, "theirs")).thenReturn(file(FIRST_FILE));
        when(repository.checkHistory(FIRST_FILE, "base")).thenReturn(file(FIRST_FILE));
        when(repository.checkHistory(SECOND_FILE, "base")).thenReturn(file(SECOND_FILE));

        var result = context.service().getConflictDetails(context.conflict());

        assertTrue(result.oursRevision().exists());
        assertTrue(result.theirsRevision().exists());
        assertFalse(result.fileAvailability().get(FIRST_FILE).ours());
        assertTrue(result.fileAvailability().get(FIRST_FILE).theirs());
        assertTrue(result.fileAvailability().get(FIRST_FILE).base());
        assertTrue(result.fileAvailability().get(SECOND_FILE).ours());
        assertFalse(result.fileAvailability().get(SECOND_FILE).theirs());
        assertTrue(result.fileAvailability().get(SECOND_FILE).base());
    }

    @Test
    void propagatesRepositoryFailureInsteadOfReportingDeletedFiles() throws Exception {
        var context = createContext();
        when(context.repository().checkHistory(anyString(), anyString()))
                .thenThrow(new IOException("Repository is unavailable"));

        assertThrows(IOException.class, () -> context.service().getConflictDetails(context.conflict()));
    }

    @Test
    void rejectsDownloadWhenFileDoesNotExistInRequestedRevision() throws Exception {
        var context = createContext();
        when(context.repository().readHistory(FIRST_FILE, "ours")).thenReturn(null);
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(NotFoundException.class,
                () -> service.getConflictFileItem(conflict, FIRST_FILE, ConflictBase.OURS));

        assertEquals("openl.error.404.project.merge.conflict.file.revision.not.found", exception.getErrorCode());
        assertArrayEquals(new Object[]{"ours", FIRST_FILE}, exception.getArgs());
    }

    @Test
    void aResolvedMergeTellsWhatItChangedAgainstTheProjectsOfTheTargetBefore() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = createContext(mergeChangesService);
        var repository = context.repository();
        givenTheirVersions(repository);
        var outcome = new MergeOutcome("before", "after",
                List.of(new FileChange("Project/" + FIRST_FILE, FileChange.Kind.MODIFIED)), List.of());
        when(repository.merge(eq("feature"), any(), any())).thenReturn(outcome);
        var project = mock(AProject.class);
        var before = List.of(project);
        when(mergeChangesService.projectsBefore("design", "main")).thenReturn(before);
        var changes = new MergedChanges("feature", "main", outcome, before, before);
        when(mergeChangesService.changesOf("design", "feature", "main", outcome, before)).thenReturn(changes);
        var summary = new MergeSummaryView("feature", "main", "before", "after", List.of(), 0);
        when(mergeChangesService.summaryOf(context.conflict().project(), changes)).thenReturn(summary);

        var response = resolveBothWithTheirs(context);

        assertEquals(summary, response.summary());
    }

    /**
     * The resolution is committed before the index is asked to publish it, so an index that does not publish it
     * leaves the resolution a success, only without a summary: its projects after the merge would be the ones before.
     */
    @Test
    void anIndexThatDoesNotPublishTheResolutionLeavesItWithoutASummary() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = createContext(mergeChangesService);
        var repository = context.repository();
        when(repository.supports().branches()).thenReturn(true);
        when(context.designTimeRepository().refreshBranch("design", "main"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("index is busy")));
        givenTheirVersions(repository);
        when(repository.merge(eq("feature"), any(), any())).thenReturn(new MergeOutcome("before", "after",
                List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), List.of()));

        var response = resolveBothWithTheirs(context);

        assertEquals(ConflictResolutionStatus.SUCCESS, response.status());
        assertNull(response.summary());
        verify(mergeChangesService, never()).changesOf(any(), any(), any(), any(), any());
    }

    /** A descriptor declaring the module of the resolved workbook already is not written again. */
    @Test
    void aDescriptorDeclaringTheModuleAlreadyIsLeftAsItIs() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = givenResolutionOfADeclaredModule(mergeChangesService, "DeletedOurs.xlsx");
        var outcomes = capturedOutcomes(mergeChangesService);

        resolveBothWithTheirs(context);

        verify(context.repository(), never()).save(any(FileData.class), any(), any());
        assertEquals("after", outcomes.getValue().after());
        assertEquals(List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), outcomes.getValue().changes());
    }

    /**
     * Descriptors whose save tells no revision are left out of the summary: compared at the merge commit, they would
     * show no change.
     */
    @Test
    void descriptorsWhoseSaveTellsNoRevisionAreLeftOutOfTheSummary() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = givenResolutionOfADeclaredModule(mergeChangesService, "Other.xlsx");
        var outcomes = capturedOutcomes(mergeChangesService);

        resolveBothWithTheirs(context);

        verify(context.repository()).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
        assertEquals("after", outcomes.getValue().after());
        assertEquals(List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), outcomes.getValue().changes());
    }

    /**
     * A resolution declares the module of a resolved workbook again in a commit of its own: the summary counts the
     * descriptor among the changed files, and its revision after the merge is the commit that wrote it.
     */
    @Test
    void aDescriptorWrittenAfterTheResolutionBelongsToTheMerge() throws Exception {
        var mergeChangesService = mock(MergeChangesService.class);
        var context = givenResolutionOfADeclaredModule(mergeChangesService, "Other.xlsx");
        var saved = new FileData();
        saved.setVersion("descriptors");
        when(context.repository().save(any(FileData.class), any(), eq(ChangesetType.DIFF))).thenReturn(saved);
        var outcomes = capturedOutcomes(mergeChangesService);

        resolveBothWithTheirs(context);

        var outcome = outcomes.getValue();
        assertEquals("before", outcome.before());
        assertEquals("descriptors", outcome.after());
        assertEquals(List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED),
                new FileChange("rules/rules.xml", FileChange.Kind.MODIFIED)), outcome.changes());
    }

    /** Their version of every conflicted file reads empty. */
    private static void givenTheirVersions(BranchRepository repository) throws IOException {
        when(repository.readHistory(anyString(), eq("theirs")))
                .thenAnswer(invocation -> new FileItem(invocation.<String>getArgument(0),
                        new ByteArrayInputStream(new byte[0])));
    }

    /**
     * A resolution of a workbook their rules.xml declares as a module, into a branch whose rules.xml declares the
     * given module.
     */
    private TestContext givenResolutionOfADeclaredModule(MergeChangesService mergeChangesService,
                                                         String targetModule) throws IOException {
        var context = createContext(mergeChangesService);
        var repository = context.repository();
        givenTheirVersions(repository);
        when(repository.readHistory("rules/rules.xml", "theirs")).thenAnswer(invocation -> new FileItem(
                "rules/rules.xml", new ByteArrayInputStream(descriptor("DeletedOurs.xlsx").getBytes())));
        when(repository.read("rules/rules.xml")).thenAnswer(invocation -> new FileItem("rules/rules.xml",
                new ByteArrayInputStream(descriptor(targetModule).getBytes())));
        var resolved = mock(RulesProject.class);
        when(resolved.getRealPath()).thenReturn("rules");
        when(context.workspace().getProjectByPath("design", FIRST_FILE)).thenReturn(Optional.of(resolved));
        when(repository.merge(eq("feature"), any(), any())).thenReturn(new MergeOutcome("before", "after",
                List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), List.of()));
        return context;
    }

    /** The merge outcome the resolution tells what it changed by. */
    private static ArgumentCaptor<MergeOutcome> capturedOutcomes(MergeChangesService mergeChangesService) {
        var outcomes = ArgumentCaptor.forClass(MergeOutcome.class);
        var changes = mock(MergedChanges.class);
        when(mergeChangesService.changesOf(eq("design"), eq("feature"), eq("main"), outcomes.capture(), any()))
                .thenReturn(changes);
        return outcomes;
    }

    private static ResolveConflictsResponse resolveBothWithTheirs(TestContext context) throws Exception {
        return context.service().resolveConflicts(context.conflict(),
                List.of(new FileConflictResolution(FIRST_FILE, ConflictResolutionStrategy.THEIRS),
                        new FileConflictResolution(SECOND_FILE, ConflictResolutionStrategy.THEIRS)),
                Map.of(),
                "Resolved");
    }

    private static String descriptor(String module) {
        return """
                <project>
                    <name>Rules</name>
                    <modules>
                        <module>
                            <name>Main</name>
                            <rules-root path="%s"/>
                        </module>
                    </modules>
                </project>
                """.formatted(module);
    }

    private TestContext createContext() throws IOException {
        return createContext(mock(MergeChangesService.class));
    }

    private TestContext createContext(MergeChangesService mergeChangesService) throws IOException {
        var repository = mock(BranchRepository.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        var workspace = mock(UserWorkspace.class);
        var project = mock(RulesProject.class);
        var features = mock(Features.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(features);
        when(repository.forBranch("main")).thenReturn(repository);
        when(project.getRepository()).thenReturn(repository);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var user = mock(WorkspaceUser.class);
        lenient().when(workspace.getUser()).thenReturn(user);
        var localWorkspace = mock(LocalWorkspace.class);
        lenient().when(workspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(workspace.getProjectByPath(anyString(), anyString())).thenReturn(Optional.empty());
        when(designTimeRepository.getRulesLocation()).thenReturn("");
        when(designTimeRepository.getRepository("design")).thenReturn(repository);

        var diffs = new LinkedHashMap<String, String>();
        diffs.put(FIRST_FILE, "diff");
        diffs.put(SECOND_FILE, "diff");
        var conflictDetails = MergeConflictDetails.builder()
                .diffs(diffs)
                .yourCommit("ours")
                .theirCommit("theirs")
                .baseCommit("base")
                .build();
        var conflict = MergeConflictInfo.builder()
                .details(conflictDetails)
                .project(project)
                .mergeBranchFrom("feature")
                .mergeBranchTo("main")
                .currentBranch("main")
                .build();
        var service = new ProjectsMergeConflictsServiceImpl(mergeChangesService) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return workspace;
            }
        };
        return new TestContext(repository, designTimeRepository, workspace, conflict, service);
    }

    private static FileData file(String name) {
        var file = new FileData();
        file.setName(name);
        return file;
    }

    private record TestContext(BranchRepository repository,
                               DesignTimeRepository designTimeRepository,
                               UserWorkspace workspace,
                               MergeConflictInfo conflict,
                               ProjectsMergeConflictsServiceImpl service) {
    }
}
