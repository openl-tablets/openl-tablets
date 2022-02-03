package org.openl.rules.repository.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.FileData;

public class HistoryForRevertedCommitTest {

    private static final String REPO_URI = "target/test-classes/repositories/HistoryForRevertedCommitTest";

    private GitRepository repo;

    @Before
    public void setUp() throws GitAPIException, IOException {
        File gitRepo = new File(REPO_URI, ".git");
        if (!gitRepo.exists()) {
            File designGit = new File(REPO_URI, "git");
            assertTrue(designGit.renameTo(new File(REPO_URI, ".git")));
        }
        repo = createRepository();
    }

    @After
    public void tearDown() {
        if (repo != null) {
            repo.close();
        }
    }

    @Test
    public void testHistoryForProject1() throws IOException {
        // 1) file1 in project1 was modified in branch "project1".
        // 2) file2 in project1 was modified in branch "modify-project1".
        // 3) last commit in branch "modify-project1" was reverted.
        // 4) branch "modify-project1" was merged into branch "project1".
        //
        // Although result of "modify-project1" doesn't introduce any changes to project (commit was reverted), we show
        // them in history because they contain project modification commits. Also, we should show Merge commit, because
        // it contains the latest project state, and it differs from the latest commit in modify-project1.
        repo = repo.forBranch("project1");
        List<FileData> history = repo.listHistory("project1");
        assertEquals(5, history.size());
        assertEquals("Add project1\n", history.get(0).getComment());
        assertEquals("Hello\n", history.get(1).getComment());
        assertEquals("world\n", history.get(2).getComment());
        assertEquals("Revert \"world\"\n", history.get(3).getComment());
        assertEquals("Merge branch 'modify-project1' into project1\n", history.get(4).getComment());
    }

    @Test
    public void testHistoryForProject2() throws IOException {
        // 1) project2 was modified in branch "project2".
        // 2) branch "project1" was merged into branch "project2".
        //
        // Branch "project1" doesn't contain commits related to project2, merge commit shouldn't appear in history.
        repo = repo.forBranch("project2");
        List<FileData> history = repo.listHistory("project2");
        assertEquals(2, history.size());
        assertEquals("Add project2\n", history.get(0).getComment());
        assertEquals("Modify project2\n", history.get(1).getComment());
    }

    @Test
    public void testHistoryForProject3() throws IOException {
        // This test is similar to testHistoryForProject1, but it tests that extra commits not related to the project
        // should not break the logic if the branch contains some commit that was reverted in the same branch.
        //
        // 1) file4 in project3 was modified in branch "project3".
        // 2) file5 in project3 was modified in branch "improve-project3".
        // 3) branch "project1" was merged into branch "improve-project3"
        // 4) the commit with file5 modification in branch "improve-project3" was reverted.
        // 5) branch "project2" was merged into branch "improve-project3"
        // 4) branch "improve-project3" was merged into branch "project3".
        //
        // Although result of "improve-project3" doesn't introduce any changes to project (commit was reverted), we show
        // them in history because they contain project modification commits. Also, we should show Merge commit, because
        // it contains the latest project state, and it differs from the latest commit in improve-project3.
        repo = repo.forBranch("project3");
        List<FileData> history = repo.listHistory("project3");
        assertEquals(5, history.size());
        assertEquals("Add project3\n", history.get(0).getComment());
        assertEquals("Modify project3\n", history.get(1).getComment());
        assertEquals("Improve project3\n", history.get(2).getComment());
        assertEquals("Revert \"Improve project3\"\n", history.get(3).getComment());
        assertEquals("Merge branch 'improve-project3' into project3\n", history.get(4).getComment());
    }

    private GitRepository createRepository() {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(new File(REPO_URI).getAbsolutePath());
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.setGcAutoDetach(false);
        repo.setBranch("main");
        repo.initialize();
        return repo;
    }
}
