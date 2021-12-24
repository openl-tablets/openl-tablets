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

public class MergeCommitVersionsTest {

    private static final String REPO_URI = "target/test-classes/repositories/MergeCommitVersionsTest";

    private GitRepository repo;

    @Before
    public void setUp() throws GitAPIException, IOException {
        File gitRepo = new File(REPO_URI, ".git");
        if (!gitRepo.exists()) {
            File designGit = new File(REPO_URI, "design-git");
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
    public void testMergeCommitVersions() throws IOException {
        List<FileData> sampleProjectHistory = repo.listHistory("DESIGN/rules/Sample Project");
        List<FileData> example3History = repo.listHistory("DESIGN/rules/Example 3 - Auto Policy Calculation");
        assertEquals(4, sampleProjectHistory.size());
        assertEquals(4, example3History.size());
        String example3Version = example3History.get(3).getVersion();
        assertEquals("15d64562c3eba9e667c4f5832458b9d3b06a325f", example3Version);
        String sampleProjectVersion = sampleProjectHistory.get(3).getVersion();
        assertEquals("03216a3a5539660a5c9ab32ac3ad9d71862c6337", sampleProjectVersion);
    }

    private GitRepository createRepository() {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(new File(REPO_URI).getAbsolutePath());
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.setGcAutoDetach(false);
        repo.initialize();
        return repo;
    }
}
