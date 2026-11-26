package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FileData;

class MergeCommitVersionsTest {

    private static final String REPO_URI = "target/test-classes/repositories/MergeCommitVersionsTest";

    private GitRepository repo;

    @BeforeEach
    void setUp() {
        var gitRepo = new File(REPO_URI, ".git");
        if (!gitRepo.exists()) {
            var designGit = new File(REPO_URI, "design-git");
            assertTrue(designGit.renameTo(new File(REPO_URI, ".git")));
        }
        repo = createRepository();
    }

    @AfterEach
    void tearDown() {
        if (repo != null) {
            repo.close();
        }
    }

    @Test
    void testMergeCommitVersions() throws IOException {
        // The changes of each project, the head of the branch it stands on aside when it changes nothing
        var sampleProjectHistory = changesOf("DESIGN/rules/Sample Project");
        var example3History = changesOf("DESIGN/rules/Example 3 - Auto Policy Calculation");
        assertEquals(4, sampleProjectHistory.size());
        assertEquals(4, example3History.size());
        var example3Version = example3History.get(3).getVersion();
        assertEquals("15d64562c3eba9e667c4f5832458b9d3b06a325f", example3Version);
        var sampleProjectVersion = sampleProjectHistory.get(3).getVersion();
        assertEquals("03216a3a5539660a5c9ab32ac3ad9d71862c6337", sampleProjectVersion);
    }

    private List<FileData> changesOf(String project) throws IOException {
        return TestGitUtils.changes(repo.listHistory(project));
    }

    private GitRepository createRepository() {
        var newRepo = new GitRepository();
        newRepo.setId("design");
        newRepo.setUri(new File(REPO_URI).getAbsolutePath());
        newRepo.setGcAutoDetach(false);
        newRepo.initialize(new GitRootFactory());
        return newRepo;
    }
}
