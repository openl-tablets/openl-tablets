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
        List<FileData> javaPrHistory = repo.listHistory("DESIGN/rules/javaPr");
        assertEquals(4, sampleProjectHistory.size());
        assertEquals(4, javaPrHistory.size());
        String javaPrVersion = javaPrHistory.get(3).getVersion();
        assertEquals("3e8503c3573375628e6f7b8bd50f3c326961e68f", javaPrVersion);
        String sampleProjectVersion = sampleProjectHistory.get(3).getVersion();
        assertEquals("887c338089201a2ef5fb4c428c8fff3ddbbbfcb3", sampleProjectVersion);
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
