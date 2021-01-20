package org.openl.rules.repository.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openl.rules.repository.git.TestGitUtils.assertContains;
import static org.openl.rules.repository.git.TestGitUtils.createFileData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.ObjectDirectory;
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
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class LocalGitRepositoryTest {
    private static final String FOLDER_IN_REPOSITORY = "rules/project1";

    private File root;
    private GitRepository repo;

    @Before
    public void setUp() throws IOException {
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
    public void testCreatePackFolderAfterGC() throws IOException {
        File packDirectory;
        try (Git git = repo.getClosableGit()) {
            packDirectory = ((ObjectDirectory) git.getRepository().getObjectDatabase()).getPackDirectory();
        }
        assertTrue(packDirectory.delete());
        assertFalse(packDirectory.exists());

        List<FileItem> changes = Collections
            .singletonList(new FileItem("rules/project1/file2", IOUtils.toInputStream("Modified")));

        FileData folderData = new FileData();
        folderData.setName("rules/project1");
        folderData.setAuthor("John Smith");

        // git.gc() is invoked inside repo.save()
        FileData savedData = repo.save(folderData, changes, ChangesetType.FULL);
        assertNotNull(savedData);
        assertTrue(packDirectory.exists());
    }

    @Test
    public void testBranches() throws IOException {
        try {
            repo.createBranch(FOLDER_IN_REPOSITORY, "project1/test1");
            fail("Must fail when create a branch on empty repository");
        } catch (IOException e) {
            assertEquals("Can't create a branch on the empty repository.", e.getMessage());
        }

        String text = "Some text";
        repo.save(createFileData("initial.txt", text), IOUtils.toInputStream(text));

        repo.createBranch(FOLDER_IN_REPOSITORY, "project1/test1");
        repo.createBranch(FOLDER_IN_REPOSITORY, "project1/test2");
        List<String> branches = repo.getBranches(FOLDER_IN_REPOSITORY);
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

        repo.createBranch(FOLDER_IN_REPOSITORY, "branch1");

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
        repo.createBranch(FOLDER_IN_REPOSITORY, "branch1");

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
    public void testDiffWhenConflictInFileWithParenthesis() throws IOException {
        final String project1 = "rules/project(1)";
        final String file = project1 + "/file1";
        final String textInMaster = "In master";
        final String textInBranch1 = "In branch1";

        writeSampleFile(repo, file, "Project(1) was created");
        repo.createBranch(FOLDER_IN_REPOSITORY, "branch1");

        writeSampleFile(repo, file, textInMaster, "Modify master");
        writeSampleFile(repo.forBranch("branch1"), file, textInBranch1, "Modify branch1");
        try {
            repo.merge("branch1", "admin", null);
            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            String diff = e.getDiffs().get(file);
            assertNotNull(diff);
            assertTrue(diff.contains("--- \"a/rules/project(1)/file1\""));
            assertTrue(diff.contains("+++ \"b/rules/project(1)/file1\""));
        }
    }

    @Test
    public void testHistoryWhenMergeWithConflictAndChooseYours() throws IOException, GitAPIException {
        final String project1 = "rules/project1";
        final String file = project1 + "/file1";
        final String textInMaster = "In master";
        final String textInBranch1 = "In branch1";

        writeSampleFile(repo, file, "Project1 was created");
        repo.createBranch(FOLDER_IN_REPOSITORY, "branch1");

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

    @Test
    public void testHistoryWhenMergeDifferentProjectModifications() throws IOException {
        writeSampleFile(repo, "rules/project1/file1", "Project1 was created");
        writeSampleFile(repo, "rules/project2/file1", "Project2 was created");
        assertEquals(1, repo.listHistory("rules/project1").size());
        assertEquals(1, repo.listHistory("rules/project2").size());

        String branch1 = "branch1";
        repo.createBranch(FOLDER_IN_REPOSITORY, branch1);
        GitRepository repoForBranch1 = repo.forBranch(branch1);

        modifyFile(repo, "rules/project2/file1", "Modify project2 in 'master'");
        modifyFile(repoForBranch1, "rules/project1/file1", "Modify project1 in 'branch1'");

        assertEquals(1, repo.listHistory("rules/project1").size());
        assertEquals(2, repoForBranch1.listHistory("rules/project1").size());
        assertEquals(2, repo.listHistory("rules/project2").size());

        repoForBranch1.merge(repo.getBranch(), "user1", null);
        repo.merge(repoForBranch1.getBranch(), "user1", null);

        List<FileData> historyForProject2 = repo.listHistory("rules/project2");
        // See EPBDS-10480 for details.
        assertEquals(2, historyForProject2.size());
    }

    @Test
    public void testIsMergedWhenNoValuableCommitsInOtherBranch() throws IOException {
        final String mainBranch = repo.getBranch();
        final String branch1 = "branch1";

        writeSampleFile(repo, "rules/project1/file1", "Project1 was created");
        writeSampleFile(repo, "rules/project2/file1", "Project2 was created");

        repo.createBranch(FOLDER_IN_REPOSITORY, branch1);

        GitRepository repoBranch1 = repo.forBranch(branch1);
        assertTrue(repo.isMergedInto(branch1, mainBranch));
        assertTrue(repo.isMergedInto(mainBranch, branch1));

        // Modify a file in branch1.
        modifyFile(repoBranch1, "rules/project1/file1", "Modify 'file1' in the branch 'branch1'. #1");
        assertFalse(repo.isMergedInto(branch1, mainBranch));
        assertTrue(repo.isMergedInto(mainBranch, branch1));

        // Merge changes to main branch.
        repo.merge(branch1, "admin", null);
        assertTrue(repo.isMergedInto(branch1, mainBranch));
        assertTrue(repo.isMergedInto(mainBranch, branch1));

        // Modify a file in branch1 again. We have 1 extra merge commit in main branch.
        modifyFile(repoBranch1, "rules/project1/file1", "Modify 'file1' in the branch 'branch1'. #2");
        assertFalse(repo.isMergedInto(branch1, mainBranch));
        // See PBDS-10808. In main branch there are no valuable changes. Only merge commits gotten from branch1. So we
        // assume that there are no interesting commits.
        assertTrue(repo.isMergedInto(mainBranch, branch1));

        // Merge changes to main branch
        repo.merge(branch1, "admin", null);
        assertTrue(repo.isMergedInto(branch1, mainBranch));
        assertTrue(repo.isMergedInto(mainBranch, branch1));

        // We modify it 3 times to ensure that it still works for 2 extra merge commits in main branch (to ensure that
        // our algorithm works recursively).
        modifyFile(repoBranch1, "rules/project1/file1", "Modify 'file1' in the branch 'branch1'. #3");
        assertFalse(repo.isMergedInto(branch1, mainBranch));
        assertTrue(repo.isMergedInto(mainBranch, branch1));
    }

    @Test
    public void testIsMergedWhenValuableCommitInOtherBranchWasDiscarded() throws IOException {
        final String mainBranch = repo.getBranch();
        final String branch1 = "branch1";
        final String branch2 = "branch2";

        writeSampleFile(repo, "rules/project1/file1", "Project1 was created");
        writeSampleFile(repo, "rules/project2/file1", "Project2 was created");

        repo.createBranch(FOLDER_IN_REPOSITORY, branch1);
        repo.createBranch(FOLDER_IN_REPOSITORY, branch2);

        GitRepository repoBranch1 = repo.forBranch(branch1);
        GitRepository repoBranch2 = repo.forBranch(branch2);

        // Modify a file in branch1 and merge it to main branch.
        final String textInBranch1 = "Modify 'file1' in the branch 'branch1'. #1";
        modifyFile(repoBranch1, "rules/project1/file1", textInBranch1);
        repo.merge(branch1, "admin", null);
        assertTrue(repo.isMergedInto(branch1, mainBranch));
        assertTrue(repo.isMergedInto(mainBranch, branch1));

        // Modify a file in branch2 and merge it to main branch with conflict. Choose theirs.
        final String textInBranch2 = "Modify 'file1' in the branch 'branch2'.";
        modifyFile(repoBranch2, "rules/project1/file1", textInBranch2);
        try {
            repo.merge(branch2, "admin", null);
            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            final String resolveMessage = "Resolve conflict (use theirs)";
            Iterable<FileItem> resolvedFiles = Collections
                .singletonList(new FileItem("rules/project1/file1", IOUtils.toInputStream(textInBranch1)));

            // Resolve conflict with choosing "theirs".
            repo.merge(branch2, "admin", new ConflictResolveData(e.getTheirCommit(), resolvedFiles, resolveMessage));
            assertTrue(repo.isMergedInto(branch2, mainBranch));
            // Because it was a conflict, project state in mainBranch differs from the state in branch2
            assertFalse(repo.isMergedInto(mainBranch, branch2));

            assertTrue(repo.isMergedInto(branch1, mainBranch));
            // Our project (project1) was modified in branch2 and then their changes were discarded when merged into
            // main. We should be able to retrieve their changes despite that they were discarded.
            // So we expect that main branch isn't merged into branch1 (there are valuable changes in main branch).
            assertFalse(repo.isMergedInto(mainBranch, branch1));

            // Modify again in branch1.
            modifyFile(repoBranch1, "rules/project1/file1", "Modify 'file1' in the branch 'branch1'. #2");
            assertFalse(repo.isMergedInto(branch1, mainBranch));
            assertFalse(repo.isMergedInto(mainBranch, branch1));
        }
    }

    private void modifyFile(GitRepository repository, String path, String text) throws IOException {
        String comment = "'" + path + "' in the branch '" + repository.getBranch() + "' was modified";
        writeSampleFile(repository, path, text, comment);
    }

    private void writeSampleFile(Repository repository, String path, String comment) throws IOException {
        String text = "File located in " + path;
        writeSampleFile(repository, path, text, comment);
    }

    private void writeSampleFile(Repository repository, String path, String text, String comment) throws IOException {
        repository.save(createFileData(path, text, comment), IOUtils.toInputStream(text));
    }

    private GitRepository createRepository(File local) {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        FileSystemRepository settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(local.getParent() + "/git-settings");
        String locksRoot = new File(root, "locks").getAbsolutePath();
        repo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.setGcAutoDetach(false);
        repo.initialize();

        return repo;
    }
}
