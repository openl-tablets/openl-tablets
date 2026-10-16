package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.MergedWorkbook;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.studio.projects.service.tables.UploadFormatting;

class MergedWorkbookFormattingTest {

    private static final String MAIN = "Rates/rules/Main.xlsx";
    private static final String OTHER = "Other/rules/Main.xlsx";
    private static final byte[] MERGED = {1};
    private static final byte[] FORMATTED = {3};

    private UploadFormatting uploadFormatting;
    private RulesProject project;
    private WebStudio studio;
    private BranchRepository root;
    private BranchRepository target;
    private MergedWorkbookFormatting.CommittedMerge merge;
    private MergedWorkbookFormatting formatting;

    @BeforeEach
    void setUp() throws IOException {
        uploadFormatting = mock(UploadFormatting.class);
        when(uploadFormatting.formats(any())).thenAnswer(invocation -> !invocation.<Collection<?>>getArgument(0)
                .isEmpty());
        project = mock(RulesProject.class);
        when(project.getRealPath()).thenReturn("Rates");
        studio = mock(WebStudio.class);
        root = mock(BranchRepository.class);
        target = mock(BranchRepository.class);
        when(root.forBranch("master")).thenReturn(target);
        when(target.readHistory(MAIN, "a1")).thenAnswer(invocation -> file(MAIN, MERGED));
        merge = new MergedWorkbookFormatting.CommittedMerge("dev", "master", mock(UserInfo.class), studio);
        formatting = new MergedWorkbookFormatting(uploadFormatting);
    }

    @Test
    void formatsTheWorkbooksOfTheProjectByTheirPathsInIt() {
        var other = new byte[]{2};
        when(uploadFormatting.formatStaged(project, "", Map.of("rules/Main.xlsx", MERGED), false, studio))
                .thenReturn(Map.of("rules/Main.xlsx", FORMATTED));
        var workbooks = new LinkedHashMap<String, byte[]>();
        workbooks.put(MAIN, MERGED);
        workbooks.put(OTHER, other);

        var result = formatting.format(project, workbooks, studio);

        assertArrayEquals(FORMATTED, result.get(MAIN));
        assertSame(other, result.get(OTHER));
    }

    @Test
    void leavesTheWorkbooksOfOtherProjectsAsTheyAre() {
        Map<String, byte[]> workbooks = Map.of(OTHER, new byte[]{2});

        assertSame(workbooks, formatting.format(project, workbooks, studio));
        verify(uploadFormatting, never())
                .formatStaged(any(AProjectFolder.class), anyString(), any(), anyBoolean(), eq(studio));
    }

    /** The workbooks of the project are formatted after the merge and committed on it, the others left alone. */
    @Test
    void commitsTheFormattedWorkbooksOfACommittedMergeOnTheMerge() throws IOException {
        givenFormatted(FORMATTED);
        var saved = new FileData();
        saved.setVersion("a2");
        when(target.save(any(FileData.class), any(), eq(ChangesetType.DIFF))).thenReturn(saved);
        var outcome = outcome(new FileChange(MAIN, FileChange.Kind.MODIFIED));

        var result = formatting.formatCommitted(project, mapped(), outcome, merge);

        assertEquals(new MergeOutcome("b1", "a2", outcome.changes(), outcome.workbooks()), result);
        var folderData = ArgumentCaptor.forClass(FileData.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> files = ArgumentCaptor.forClass(List.class);
        verify(target).save(folderData.capture(), files.capture(), eq(ChangesetType.DIFF));
        assertEquals("Rates", folderData.getValue().getName());
        assertEquals("a1", folderData.getValue().getVersion(), "The commit is based on the merge");
        assertEquals("master", folderData.getValue().getBranch());
        assertEquals(merge.author(), folderData.getValue().getAuthor());
        assertEquals(List.of(MAIN), files.getValue().stream().map(file -> file.getData().getName()).toList());
        assertArrayEquals(FORMATTED, files.getValue().getFirst().getStream().readAllBytes());
        verify(target, never()).readHistory(eq(OTHER), any());
    }

    @Test
    void countsAWorkbookTheMergeLeftAsItWasAmongTheChangesOnceFormatted() throws IOException {
        givenFormatted(FORMATTED);
        var saved = new FileData();
        saved.setVersion("a2");
        when(target.save(any(FileData.class), any(), eq(ChangesetType.DIFF))).thenReturn(saved);

        var result = formatting.formatCommitted(project, mapped(), outcome(), merge);

        assertEquals(List.of(new FileChange(MAIN, FileChange.Kind.MODIFIED)), result.changes());
    }

    @Test
    void commitsNothingWhenTheFormattingLeavesTheWorkbooksAsMerged() throws IOException {
        givenFormatted(MERGED.clone());
        var outcome = outcome();

        assertSame(outcome, formatting.formatCommitted(project, mapped(), outcome, merge));
        verify(target, never()).save(any(FileData.class), any(), any());
    }

    @Test
    void readsNothingWhileTablesAreNotFormattedOnSave() throws IOException {
        doReturn(false).when(uploadFormatting).formats(any());
        var outcome = outcome();

        assertSame(outcome, formatting.formatCommitted(project, mapped(), outcome, merge));
        verify(root, never()).forBranch(any());
    }

    @Test
    void leavesTheMergeAsCommittedWhenTheFormattedWorkbooksCannotBeCommitted() throws IOException {
        givenFormatted(FORMATTED);
        when(target.save(any(FileData.class), any(), eq(ChangesetType.DIFF))).thenThrow(new IOException("Changed"));
        var outcome = outcome();

        assertSame(outcome, formatting.formatCommitted(project, mapped(), outcome, merge));
    }

    @Test
    void readsAProjectOfAnotherBranchByThePathsFromTheRootOfTheRepository() throws IOException {
        var onBranch = MergedWorkbookFormatting.onBranch(project, mapped(), "master");

        assertSame(target, onBranch.getRepository());
        assertEquals("Rates", onBranch.getFolderPath());
    }

    private void givenFormatted(byte[] workbook) {
        when(uploadFormatting.formatStaged(any(AProject.class), eq(""), any(), eq(false), eq(studio)))
                .thenAnswer(invocation -> {
                    var staged = invocation.<AProject>getArgument(0);
                    assertSame(target, staged.getRepository(), "The project is staged as the merge left it");
                    assertEquals("a1", staged.getHistoryVersion());
                    var workbooks = invocation.<Map<String, byte[]>>getArgument(2);
                    assertEquals(List.of("rules/Main.xlsx"), List.copyOf(workbooks.keySet()));
                    assertArrayEquals(MERGED, workbooks.get("rules/Main.xlsx"));
                    return Map.of("rules/Main.xlsx", workbook);
                });
    }

    /** A merge into the branch that combined a workbook of the project and one of another project. */
    private static MergeOutcome outcome(FileChange... changes) {
        return new MergeOutcome("b1",
                "a1",
                List.of(changes),
                List.of(new MergedWorkbook(MAIN, List.of(), List.of(), List.of("Rates")),
                        new MergedWorkbook(OTHER, List.of(), List.of(), List.of("Rates"))));
    }

    /** The repository of the project, its folders mapped, behind a wrapper. */
    private BranchRepository mapped() {
        var mapper = mock(BranchRepository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(((FolderMapper) mapper).getDelegate()).thenReturn(root);
        var wrapper = mock(BranchRepository.class, withSettings().extraInterfaces(RepositoryDelegate.class));
        when(((RepositoryDelegate) wrapper).getOriginal()).thenReturn(mapper);
        return wrapper;
    }

    private static FileItem file(String path, byte[] content) {
        return new FileItem(path, new ByteArrayInputStream(content));
    }
}
