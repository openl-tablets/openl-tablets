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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
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
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.ConflictTextBlock;
import org.openl.studio.projects.model.merge.ConflictTextBlockKind;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeSummaryView;
import org.openl.studio.projects.model.merge.MergedChanges;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;

class ProjectsMergeConflictsServiceImplTest {

    private static final String FIRST_FILE = "rules/DeletedOurs.xlsx";
    private static final String SECOND_FILE = "rules/DeletedTheirs.xlsx";
    /** The commits of the conflict: the base, yours and theirs. */
    private static final List<String> COMMITS = List.of("base", "ours", "theirs");

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
        // A workbook is no text, whichever versions hold it.
        assertFalse(result.fileAvailability().get(FIRST_FILE).text());
        assertFalse(result.fileAvailability().get(SECOND_FILE).text());
    }

    @Test
    void tellsWhichConflictedFilesAreText() throws Exception {
        var notes = "rules/notes.txt";
        var latin1 = "rules/latin1.properties";
        var removed = "rules/removed.txt";
        var library = "lib/rates.bin";
        var context = createContext(mock(MergeChangesService.class), "base", notes, latin1, removed, library,
                FIRST_FILE);
        var repository = context.repository();
        givenTextVersions(repository, notes, "a");
        givenTextVersions(repository, library, "rate\u0000table");
        for (var commit : COMMITS) {
            when(repository.checkHistory(latin1, commit)).thenReturn(file(latin1));
            givenVersion(repository, latin1, commit, "Pr\u00e4mie=" + commit + "\n", StandardCharsets.ISO_8859_1);
        }
        // Their branch deleted the file: the versions that hold it are text all the same.
        givenTextVersions(repository, removed, "a");
        when(repository.checkHistory(removed, "theirs")).thenReturn(null);
        when(repository.readHistory(removed, "theirs")).thenReturn(null);

        var result = context.service().getConflictDetails(context.conflict());

        assertTrue(result.fileAvailability().get(notes).text());
        // A merge of lines reads UTF-8 only; the screen compares such a file line by line all the same.
        assertFalse(result.fileAvailability().get(latin1).text());
        assertTrue(result.fileAvailability().get(removed).text());
        assertFalse(result.fileAvailability().get(removed).theirs());
        assertFalse(result.fileAvailability().get(library).text());
        // A workbook is told by its name: its versions are not read.
        assertFalse(result.fileAvailability().get(FIRST_FILE).text());
        verify(repository, never()).readHistory(eq(FIRST_FILE), anyString());
    }

    @Test
    void readsEachConflictedFileOnceWhileTheConflictLasts() throws Exception {
        var notes = "rules/notes.txt";
        var context = createContext(mock(MergeChangesService.class), "base", notes);
        var repository = context.repository();
        givenTextVersions(repository, notes, "a");

        context.service().getConflictDetails(context.conflict());
        var again = context.service().getConflictDetails(context.conflict());

        assertTrue(again.fileAvailability().get(notes).text());
        for (var commit : COMMITS) {
            verify(repository, times(1)).readHistory(notes, commit);
        }
    }

    @Test
    void tellsAFileWithAVersionThatCannotBeReadAsNoTextUntilItIsRead() throws Exception {
        var notes = "rules/notes.txt";
        var other = "rules/other.txt";
        var context = createContext(mock(MergeChangesService.class), "base", notes, other);
        var repository = context.repository();
        givenTextVersions(repository, notes, "a");
        givenTextVersions(repository, other, "b");
        when(repository.readHistory(notes, "theirs"))
                .thenThrow(new IOException("The LFS object is unavailable"))
                .thenAnswer(invocation -> new FileItem(notes,
                        new ByteArrayInputStream("a=theirs\n".getBytes(StandardCharsets.UTF_8))));

        var result = context.service().getConflictDetails(context.conflict());

        // The other resolutions of the file stay offered, and the rest of the conflict is shown as it is.
        assertFalse(result.fileAvailability().get(notes).text());
        assertTrue(result.fileAvailability().get(notes).theirs());
        assertTrue(result.fileAvailability().get(other).text());
        // The failure is not kept: the file is read again the next time, and is text once its versions are read.
        assertTrue(context.service().getConflictDetails(context.conflict()).fileAvailability().get(notes).text());
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
    void mergesAConflictedTextFileLineByLine() throws Exception {
        var context = createContext();
        givenVersion(context.repository(), "base", "a=1\nb=2\nc=3\nd=4\n");
        givenVersion(context.repository(), "ours", "a=1\nb=ours\nc=3\nd=4\n");
        givenVersion(context.repository(), "theirs", "a=1\nb=theirs\nc=3\nd=theirs\n");

        var text = context.service().getConflictText(context.conflict(), FIRST_FILE);

        // Lines a branch changed come with every version of them, lines no branch changed with their text alone.
        assertEquals(List.of(
                new ConflictTextBlock(ConflictTextBlockKind.MERGED, "a=1\n", null, null, null),
                new ConflictTextBlock(ConflictTextBlockKind.CONFLICT, null, "b=ours\n", "b=2\n", "b=theirs\n"),
                new ConflictTextBlock(ConflictTextBlockKind.MERGED, "c=3\n", null, null, null),
                new ConflictTextBlock(ConflictTextBlockKind.MERGED, "d=theirs\n", "d=4\n", "d=4\n", "d=theirs\n")),
                text.blocks());
    }

    @Test
    void tellsThatHistoriesWithNoCommitInCommonHaveNoBaseVersion() throws Exception {
        var context = createContext(mock(MergeChangesService.class), null, FIRST_FILE);
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(NotFoundException.class,
                () -> service.getConflictFileItem(conflict, FIRST_FILE, ConflictBase.BASE));

        assertEquals("openl.error.404.project.merge.conflict.file.side.not.found", exception.getErrorCode());
        assertArrayEquals(new Object[]{ConflictBase.BASE, FIRST_FILE}, exception.getArgs());
    }

    @Test
    void readsABaseThatDoesNotHoldTheFileAsAnEmptyFile() throws Exception {
        var context = createContext();
        when(context.repository().readHistory(FIRST_FILE, "base")).thenReturn(null);
        givenVersion(context.repository(), "ours", "a=ours\n");
        givenVersion(context.repository(), "theirs", "a=theirs\n");

        var text = context.service().getConflictText(context.conflict(), FIRST_FILE);

        assertEquals(List.of(new ConflictTextBlock(ConflictTextBlockKind.CONFLICT, null, "a=ours\n", "", "a=theirs\n")),
                text.blocks());
    }

    @Test
    void refusesToMergeAFileThatIsNoText() throws Exception {
        var context = createContext();
        givenVersion(context.repository(), "base", "a=1\n");
        givenVersion(context.repository(), "ours", "a=1\u0000\n");
        givenVersion(context.repository(), "theirs", "a=2\n");
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(BadRequestException.class, () -> service.getConflictText(conflict, FIRST_FILE));

        assertEquals("openl.error.400.project.merge.conflict.text.not-text.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{FIRST_FILE}, exception.getArgs());
    }

    @Test
    void refusesToMergeAFileThatIsNotConflicted() throws Exception {
        var context = createContext();
        var service = context.service();
        var conflict = context.conflict();

        var exception = assertThrows(NotFoundException.class, () -> service.getConflictText(conflict, "notes.txt"));

        assertEquals("openl.error.404.project.merge.conflict.file.not.found", exception.getErrorCode());
    }

    /** Answers each read of the first conflicted file at a commit with the text, read anew. */
    private static void givenVersion(BranchRepository repository, String commit, String text) throws IOException {
        givenVersion(repository, FIRST_FILE, commit, text);
    }

    /** Answers each read of a conflicted file at a commit with the text, read anew. */
    private static void givenVersion(BranchRepository repository,
                                     String path,
                                     String commit,
                                     String text) throws IOException {
        givenVersion(repository, path, commit, text, StandardCharsets.UTF_8);
    }

    /** Answers each read of a conflicted file at a commit with the text written in the charset, read anew. */
    private static void givenVersion(BranchRepository repository,
                                     String path,
                                     String commit,
                                     String text,
                                     Charset charset) throws IOException {
        when(repository.readHistory(path, commit)).thenAnswer(invocation -> new FileItem(path,
                new ByteArrayInputStream(text.getBytes(charset))));
    }

    /** Every version of the conflict holds the file, as the text {@code <key>=<commit>}. */
    private static void givenTextVersions(BranchRepository repository, String path, String key) throws IOException {
        for (var commit : COMMITS) {
            when(repository.checkHistory(path, commit)).thenReturn(file(path));
            givenVersion(repository, path, commit, key + "=" + commit + "\n");
        }
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

    /**
     * The descriptors of a resolution belong to the branch it merged into: read or written on the default branch of
     * the repository, they would change a branch the merge never touched and miss the one it did.
     */
    @Test
    void theDescriptorsOfAResolutionAreWrittenToTheBranchItMergedInto() throws Exception {
        var context = createContext();
        var repository = context.repository();
        var target = mock(BranchRepository.class);
        var features = repository.supports();
        when(target.supports()).thenReturn(features);
        when(repository.forBranch("main")).thenReturn(target);
        when(repository.readHistory(anyString(), eq("theirs")))
                .thenAnswer(invocation -> new FileItem(invocation.<String>getArgument(0),
                        new ByteArrayInputStream(new byte[0])));
        when(repository.readHistory("rules/rules.xml", "theirs")).thenAnswer(invocation -> new FileItem(
                "rules/rules.xml", new ByteArrayInputStream(descriptor("DeletedOurs.xlsx").getBytes())));
        when(target.read("rules/rules.xml")).thenAnswer(invocation -> new FileItem("rules/rules.xml",
                new ByteArrayInputStream(descriptor("Other.xlsx").getBytes())));
        var resolved = mock(RulesProject.class);
        when(resolved.getRealPath()).thenReturn("rules");
        when(context.workspace().getProjectByPath("design", FIRST_FILE)).thenReturn(Optional.of(resolved));
        when(target.merge(eq("feature"), any(), any())).thenReturn(new MergeOutcome("before", "after",
                List.of(new FileChange(FIRST_FILE, FileChange.Kind.MODIFIED)), List.of()));

        context.service().resolveConflicts(context.conflict(),
                List.of(new FileConflictResolution(FIRST_FILE, ConflictResolutionStrategy.THEIRS),
                        new FileConflictResolution(SECOND_FILE, ConflictResolutionStrategy.THEIRS)),
                Map.of(),
                "Resolved");

        verify(target).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
        verify(repository, never()).read("rules/rules.xml");
        verify(repository, never()).save(any(FileData.class), any(), any());
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
        return createContext(mergeChangesService, "base", FIRST_FILE, SECOND_FILE);
    }

    /**
     * @param baseCommit      the commit the branches of the conflict parted at, or {@code null} when their histories
     *                        have none in common
     * @param conflictedFiles the files the conflict lists
     */
    private TestContext createContext(MergeChangesService mergeChangesService,
                                      @Nullable String baseCommit,
                                      String... conflictedFiles) throws IOException {
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
        Stream.of(conflictedFiles).forEach(file -> diffs.put(file, "diff"));
        var conflictDetails = MergeConflictDetails.builder()
                .diffs(diffs)
                .yourCommit("ours")
                .theirCommit("theirs")
                .baseCommit(baseCommit)
                .build();
        var conflict = MergeConflictInfo.builder()
                .details(conflictDetails)
                .project(project)
                .mergeBranchFrom("feature")
                .mergeBranchTo("main")
                .currentBranch("main")
                .build();
        // The conflict is kept for the project, as a merge or a save keeps it.
        var conflictsSessionHolder = new ProjectsMergeConflictsSessionHolder();
        conflictsSessionHolder.store(ProjectIdModel.builder().repository("design").projectName("Project").build(),
                conflict);
        var service = new ProjectsMergeConflictsServiceImpl(mergeChangesService, conflictsSessionHolder) {
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
