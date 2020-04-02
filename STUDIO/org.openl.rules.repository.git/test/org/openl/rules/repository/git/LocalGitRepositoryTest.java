package org.openl.rules.repository.git;

import static org.junit.Assert.*;
import static org.openl.rules.repository.git.TestGitUtils.assertContains;
import static org.openl.rules.repository.git.TestGitUtils.createFileData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeConflictException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class LocalGitRepositoryTest {
    private File root;
    private GitRepository repo;

    @Before
    public void setUp() throws IOException, RRepositoryException {
        root = Files.createTempDirectory("openl").toFile();
        repo = createRepository(new File(root, "design-repository"));
    }

    @After
    public void tearDown() {
        if (repo != null) {
            repo.close();
        }
        FileUtils.deleteQuietly(root);
        if (root.exists()) {
            fail("Cannot delete folder " + root);
        }
    }

    @Test
    public void testReadEmpty() throws IOException {
        // Last version
        assertEquals(0, repo.list("").size());
        assertNull(repo.check("project1"));
        assertNull(repo.read("project1/file1"));
        assertEquals(0, repo.listFolders("project1/file1").size());

        // History
        assertEquals(0, repo.listHistory("project1").size());
        assertEquals(0, repo.listFiles("project1", "not-exist").size());
        assertNull(repo.checkHistory("project1", "not-exist"));
        assertNull(repo.readHistory("project1", "not-exist"));
    }

    @Test
    public void testSaveFile() throws IOException {
        String path = "rules/project1/folder/file4";
        String text = "File located in " + path;
        FileData result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));

        assertNotNull(result);
        assertEquals(path, result.getName());
        assertEquals("John Smith", result.getAuthor());
        assertEquals("Comment for rules/project1/folder/file4", result.getComment());
        assertEquals(text.length(), result.getSize());
        assertNotNull(result.getModifiedAt());

        assertEquals(text, IOUtils.toStringAndClose(repo.read("rules/project1/folder/file4").getStream()));
    }

    @Test
    public void testSaveFolder() throws IOException {
        List<FileItem> changes = Arrays.asList(
            new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
            new FileItem("rules/project1/file2", IOUtils.toInputStream("Modified")));

        FileData folderData = new FileData();
        folderData.setName("rules/project1");
        folderData.setAuthor("John Smith");
        folderData.setComment("Bulk change");

        FileData savedData = repo.save(folderData, changes, ChangesetType.FULL);
        assertNotNull(savedData);
        List<FileData> files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file4");
        assertContains(files, "rules/project1/file2");
        assertEquals(2, files.size());
    }

    @Test
    public void testBranches() throws IOException {
        try {
            repo.createBranch("project1", "project1/test1");
            fail("Must fail when create a branch on empty repository");
        } catch (IOException e) {
            assertEquals("Can't create a branch on the empty repository.", e.getMessage());
        }

        String text = "Some text";
        repo.save(createFileData("initial.txt", text), IOUtils.toInputStream(text));

        repo.createBranch("project1", "project1/test1");
        repo.createBranch("project1", "project1/test2");
        List<String> branches = repo.getBranches("project1");
        assertTrue(branches.contains(Constants.MASTER));
        assertTrue(branches.contains("project1/test1"));
        assertTrue(branches.contains("project1/test2"));
        assertEquals(3, branches.size());
    }

    @Test
    public void testHistoryWhenMergeWithoutConflict() throws IOException {
        writeSampleFile(repo, "rules/project1/file1", "Project1 was created");
        writeSampleFile(repo, "rules/project11/file1", "Project11 was created");
        writeSampleFile(repo, "rules/project2/file1", "Project2 was created");
        assertEquals(1, repo.listHistory("rules/project1").size());
        assertEquals(1, repo.listHistory("rules/project11").size());
        assertEquals(1, repo.listHistory("rules/project2").size());

        repo.createBranch("project1", "branch1");

        writeSampleFile(repo, "rules/project1/file2", "'file2' in the branch 'master' was added");
        writeSampleFile(repo.forBranch("branch1"), "rules/project1/file3", "'file3' in the branch 'branch1' was added");

        assertFalse(repo.isMergedInto("branch1", repo.getBranch()));
        repo.merge("branch1", "admin", null);
        assertTrue(repo.isMergedInto("branch1", repo.getBranch()));
        assertFalse(repo.isMergedInto(repo.getBranch(), "branch1"));

        assertEquals(4, repo.listHistory("rules/project1").size());
        assertEquals(1, repo.listHistory("rules/project11").size());
        assertEquals(1, repo.listHistory("rules/project2").size());

        assertEquals("Merge branch 'branch1'", repo.check("rules/project1").getComment());
    }

    @Test
    public void testHistoryWhenMergeWithConflictAndChooseTheirs() throws IOException, GitAPIException {
        final String project1 = "rules/project1";
        final String file = project1 + "/file1";
        final String textInMaster = "In master";
        final String textInBranch1 = "In branch1";

        writeSampleFile(repo, file, "Project1 was created");
        repo.createBranch("project1", "branch1");

        writeSampleFile(repo, file, textInMaster, "Modify master");
        writeSampleFile(repo.forBranch("branch1"), file, textInBranch1, "Modify branch1");
        try {
            repo.merge("branch1", "admin", null);
            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            final String resolveMessage = "Resolve conflict (use theirs)";

            // !!! The text must be same as in branch1 for this test scenario. Resolve with choosing "all theirs".
            Iterable<FileItem> resolvedFiles = Collections
                .singletonList(new FileItem(file, IOUtils.toInputStream(textInBranch1)));

            repo.merge("branch1", "admin", new ConflictResolveData(e.getTheirCommit(), resolvedFiles, resolveMessage));

            assertEquals(resolveMessage, repo.check(project1).getComment());
            assertEquals(textInBranch1, IOUtils.toStringAndClose(repo.read(file).getStream()));

            assertEquals(4, repo.listHistory(project1).size());
            String lastVersion = repo.listHistory(project1).get(3).getVersion();
            assertFalse("Last commit (resolve merge conflict) is treated as old version. Must be last version.",
                repo.isCheckoutOldVersion(project1, lastVersion));
        }
    }

    @Test
    public void testHistoryWhenMergeWithConflictAndChooseYours() throws IOException, GitAPIException {
        final String project1 = "rules/project1";
        final String file = project1 + "/file1";
        final String textInMaster = "In master";
        final String textInBranch1 = "In branch1";

        writeSampleFile(repo, file, "Project1 was created");
        repo.createBranch("project1", "branch1");

        writeSampleFile(repo, file, textInMaster, "Modify master");
        writeSampleFile(repo.forBranch("branch1"), file, textInBranch1, "Modify branch1");
        try {
            repo.merge("branch1", "admin", null);
            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            final String resolveMessage = "Resolve conflict (use yours)";

            // !!! The text must be same as in master for this test scenario. Resolve with choosing "all yours".
            Iterable<FileItem> resolvedFiles = Collections
                .singletonList(new FileItem(file, IOUtils.toInputStream(textInMaster)));

            repo.merge("branch1", "admin", new ConflictResolveData(e.getTheirCommit(), resolvedFiles, resolveMessage));

            assertEquals(resolveMessage, repo.check(project1).getComment());
            assertEquals(textInMaster, IOUtils.toStringAndClose(repo.read(file).getStream()));

            assertEquals(4, repo.listHistory(project1).size());
            String lastVersion = repo.listHistory(project1).get(3).getVersion();
            assertFalse("Last commit (resolve merge conflict) is treated as old version. Must be last version.",
                repo.isCheckoutOldVersion(project1, lastVersion));
        }
    }

    private void writeSampleFile(Repository repository, String path, String comment) throws IOException {
        String text = "File located in " + path;
        writeSampleFile(repository, path, text, comment);
    }

    private void writeSampleFile(Repository repository, String path, String text, String comment) throws IOException {
        repository.save(createFileData(path, text, comment), IOUtils.toInputStream(text));
    }

    private GitRepository createRepository(File local) throws RRepositoryException {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setGitSettingsPath(local.getParent() + "/git-settings");
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.setGcAutoDetach(false);
        repo.initialize();

        return repo;
    }
}
