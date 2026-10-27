package org.openl.rules.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.MergeScope;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;

class PathCheckedRepositoryTest {

    @TempDir
    Path root;

    /** A repository on the file system that ignores the build files of Maven and the files of Git. */
    private Repository repository() throws IOException {
        write("p/rules.xml", "rules");
        write("p/Rules.xlsx", "rules workbook");
        write("p/pom.xml", "version 1");
        write("p/.git/config", "git config");
        write(".github/workflows/build.yml", "pipeline");
        return RepositoryInstatiator.newRepository("repository.design",
                Map.of("repository.design.factory", "repo-file",
                        "repository.design.uri", root.toString(),
                        IgnoredPaths.PROPERTY, "**/.git*/**, **/pom.xml")::get);
    }

    private void write(String path, String content) throws IOException {
        var file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private String contentOf(String path) throws IOException {
        return Files.readString(root.resolve(path));
    }

    private static FileItem item(String name, String content) {
        return new FileItem(name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }

    private static List<String> namesOf(List<FileData> files) {
        return files.stream().map(FileData::getName).sorted().toList();
    }

    @Test
    void neitherListsNorReadsIgnoredFiles() throws IOException {
        var repository = repository();

        assertEquals(List.of("p/Rules.xlsx", "p/rules.xml"), namesOf(repository.list("")));
        assertEquals(List.of("p/Rules.xlsx", "p/rules.xml"), namesOf(repository.listFiles("p/", null)));
        assertEquals(List.of("p"), namesOf(repository.listFolders("")));
        assertEquals(List.of(), repository.list(".github/"));
        assertNull(repository.check("p/pom.xml"));
        assertNull(repository.read("p/.git/config"));
        assertNull(repository.read("P/.GIT/CONFIG"));
        assertNull(repository.checkHistory("p/pom.xml", null));
        assertNull(repository.readHistory("p/pom.xml", null));
        assertEquals(List.of(), repository.listHistory("p/pom.xml"));
        assertNotNull(repository.check("p/rules.xml"));
    }

    @Test
    void refusesToWriteOneIgnoredFile() throws IOException {
        var repository = repository();

        assertThrows(AccessDeniedException.class,
                () -> repository.save(fileData(".GIT/hooks/pre-commit"), new ByteArrayInputStream(new byte[0])));
        assertThrows(AccessDeniedException.class,
                () -> repository.save(fileData(".github"), List.of(), ChangesetType.DIFF));
        assertThrows(AccessDeniedException.class,
                () -> repository.copyHistory("p/rules.xml", fileData("p/pom.xml"), null));
        assertThrows(FileNotFoundException.class,
                () -> repository.copyHistory("p/pom.xml", fileData("p/copy.xml"), null));
        assertFalse(Files.exists(root.resolve(".GIT/hooks/pre-commit")));
        assertEquals("version 1", contentOf("p/pom.xml"));
    }

    @Test
    void leavesIgnoredFilesOutOfAWriteOfSeveralFiles() throws IOException {
        var repository = repository();

        var saved = repository.save(List.of(item("p/new.txt", "new"), item("p/pom.xml", "version 2")));

        assertEquals(List.of("p/new.txt"), namesOf(saved));
        assertEquals("new", contentOf("p/new.txt"));
        assertEquals("version 1", contentOf("p/pom.xml"));
        assertEquals(List.of(), repository.save(List.of(item("p/pom.xml", "version 2"))));
    }

    @Test
    void neverDeletesAnIgnoredFile() throws IOException {
        var repository = repository();

        assertFalse(repository.delete(fileData("p/pom.xml")));
        assertFalse(repository.delete(List.of(fileData("p/.git/config"))));
        assertFalse(repository.deleteHistory(fileData("p/pom.xml")));
        assertTrue(Files.exists(root.resolve("p/pom.xml")));
        assertTrue(Files.exists(root.resolve("p/.git/config")));
        assertTrue(repository.delete(List.of(fileData("p/Rules.xlsx"), fileData("p/pom.xml"))));
        assertFalse(Files.exists(root.resolve("p/Rules.xlsx")));
        assertTrue(Files.exists(root.resolve("p/pom.xml")));
    }

    /**
     * A full save is given every file the folder must hold. The ignored files it holds are kept, while its other
     * files the save is not given are deleted.
     */
    @Test
    void aFullSaveKeepsTheIgnoredFilesOfTheFolder() throws IOException {
        var repository = repository();

        repository.save(fileData("p"),
                List.of(item("p/rules.xml", "new rules"), item("p/New.xlsx", "new"), item("p/pom.xml", "version 2")),
                ChangesetType.FULL);

        assertEquals("new rules", contentOf("p/rules.xml"));
        assertEquals("new", contentOf("p/New.xlsx"));
        assertFalse(Files.exists(root.resolve("p/Rules.xlsx")));
        assertEquals("version 1", contentOf("p/pom.xml"));
        assertEquals("git config", contentOf("p/.git/config"));
    }

    /**
     * A repository that deletes every file a full save is not given, as Git does, is given the changes and the
     * deletion of each other file of the version the save starts from, the ignored files left out.
     */
    @Test
    @SuppressWarnings("unchecked")
    void aFullSaveIsTurnedIntoChangesWhenTheFolderHoldsIgnoredFiles() throws IOException {
        var delegate = mock(BranchRepository.class);
        when(delegate.supports()).thenReturn(new FeaturesBuilder(delegate).build());
        when(delegate.listFiles("p/", "v1")).thenReturn(List.of(fileData("p/rules.xml"),
                fileData("p/Old.xlsx"),
                fileData("p/pom.xml")));
        var repository = new PathCheckedRepository(delegate, IgnoredPaths.of("**/pom.xml"));
        var folder = fileData("p");
        folder.setVersion("v1");

        repository.save(folder, List.of(item("p/rules.xml", "rules"), item("p/pom.xml", "2.0")), ChangesetType.FULL);

        ArgumentCaptor<Iterable<FileItem>> changes = ArgumentCaptor.forClass(Iterable.class);
        verify(delegate).save(eq(folder), changes.capture(), eq(ChangesetType.DIFF));
        var saved = new ArrayList<String>();
        changes.getValue().forEach(change -> saved.add(change.getData().getName()
                + (change.getStream() == null ? " deleted" : "")));
        assertEquals(List.of("p/rules.xml", "p/Old.xlsx deleted"), saved);
    }

    @Test
    void neitherSearchesTheHistoryOfAnIgnoredFileNorRefusesAnotherWrite() throws IOException {
        var delegate = mock(BranchRepository.class);
        var repository = new PathCheckedRepository(delegate, IgnoredPaths.of("**/pom.xml"));
        var rules = fileData("p/rules.xml");
        var content = new ByteArrayInputStream(new byte[0]);

        assertEquals(List.of(), repository.listHistory("p/pom.xml", null, true, Pageable.unpaged()));
        repository.save(rules, content);

        verify(delegate, never()).listHistory(anyString(), any(), anyBoolean(), any());
        verify(delegate).save(rules, content);
    }

    @Test
    void aFullSaveOfAFolderWithoutIgnoredFilesDeletesTheFilesItIsNotGiven() throws IOException {
        var repository = repository();
        write("q/Old.xlsx", "old");

        repository.save(fileData("q"), List.of(item("q/rules.xml", "rules")), ChangesetType.FULL);

        assertEquals("rules", contentOf("q/rules.xml"));
        assertFalse(Files.exists(root.resolve("q/Old.xlsx")));
    }

    @Test
    void ignoresOnlyTheGitFolderWhenNothingIsSet() throws IOException {
        write("p/pom.xml", "version 1");
        write("p/.git/config", "git config");
        var repository = RepositoryInstatiator.newRepository("repository.design",
                Map.of("repository.design.factory", "repo-file", "repository.design.uri", root.toString())::get);

        assertEquals(List.of("p/pom.xml"), namesOf(repository.list("")));
    }

    @Test
    void keepsBranchViewsPathChecked() throws Exception {
        var delegate = mock(BranchRepository.class);
        var target = mock(BranchRepository.class);
        when(delegate.isValidBranchName(anyString())).thenReturn(true);
        when(delegate.forBranch("feature")).thenReturn(target);
        var repository = new PathCheckedRepository(delegate, IgnoredPaths.of(null));

        var branchView = repository.forBranch("feature");

        assertInstanceOf(PathCheckedRepository.class, branchView);
        assertThrows(InvalidPathException.class, () -> branchView.list("../outside"));
        verify(target, never()).list(anyString());
    }

    @Test
    void tellsTheScopeAndTheOutcomeOfAMergeOfValidBranchesOnly() throws Exception {
        var delegate = mock(BranchRepository.class);
        when(delegate.isValidBranchName("feature")).thenReturn(true);
        when(delegate.isValidBranchName("main")).thenReturn(true);
        var rules = new FileChange("p/rules.xml", FileChange.Kind.MODIFIED);
        var pom = new FileChange("p/pom.xml", FileChange.Kind.MODIFIED);
        when(delegate.getMergeScope("feature", "main"))
                .thenReturn(new MergeScope(List.of(rules, pom), Set.of("p/rules.xml", "p/pom.xml")));
        when(delegate.merge("feature", null, null))
                .thenReturn(new MergeOutcome("before", "after", List.of(rules, pom), List.of()));
        var repository = new PathCheckedRepository(delegate, IgnoredPaths.of("**/pom.xml"));

        // The ignored files are left out of what a merge changes, as they are out of every listing.
        assertEquals(new MergeScope(List.of(rules), Set.of("p/rules.xml")),
                repository.getMergeScope("feature", "main"));
        assertEquals(new MergeOutcome("before", "after", List.of(rules), List.of()),
                repository.merge("feature", null, null));
        assertThrows(IllegalArgumentException.class, () -> repository.getMergeScope("../outside", "main"));
    }
}
