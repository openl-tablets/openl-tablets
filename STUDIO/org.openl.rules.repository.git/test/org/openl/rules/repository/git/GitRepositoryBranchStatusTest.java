package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;
import static org.openl.rules.repository.git.TestGitUtils.createNewFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

class GitRepositoryBranchStatusTest {

    private static final String BASE = "master";
    private static final String BRANCH = "branch1";
    private static final String SAME_TIP_BRANCH = "same-tip";
    private static final String REPO_ID = "design";

    @TempDir
    private static File template;
    @TempDir
    private File root;
    @AutoClose
    private GitRepository repo;

    @BeforeAll
    static void initTemplate() throws GitAPIException, IOException {
        try (Git git = Git.init().setInitialBranch(BASE).setDirectory(template).call()) {
            Repository repository = git.getRepository();
            File parent = repository.getDirectory().getParentFile();
            StoredConfig config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();
            createNewFile(parent, "file-in-master", "root");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial").setCommitter("user1", "user1@mail.to").call();
            git.branchCreate().setName(BRANCH).call();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        File remote = new File(root, "remote");
        File repositoriesFolder = new File(root, "repositories");
        File local = new File(repositoriesFolder, "local");
        FileUtils.copy(template, remote);
        repo = createRepository(remote.toURI().toString(), local, repositoriesFolder.getAbsolutePath());
    }

    @Test
    void aheadBehindAndLastCommit() throws IOException {
        // branch1 gets two commits, so it is two ahead of the shared "Initial".
        GitRepository branch = repo.forBranch(BRANCH);
        branch.save(createFileData("rules/project1/file1", "one"), IOUtils.toInputStream("one"));
        branch.save(createFileData("rules/project1/file2", "two"), IOUtils.toInputStream("two"));
        // master gets one commit after the branch point, so branch1 is one behind master.
        repo.save(createFileData("rules/project1/file3", "three"), IOUtils.toInputStream("three"));
        repo.createBranch("rules/project1", SAME_TIP_BRANCH, BASE);

        BranchStatus status = repo.getBranchStatus(BRANCH, BASE);
        assertEquals(2, status.commitsAhead(), "branch1 is two commits ahead of master");
        assertEquals(1, status.commitsBehind(), "branch1 is one commit behind master");
        assertNotNull(status.lastCommitRevision());
        assertNotNull(status.lastCommitAt());
        assertNotNull(status.lastCommitAuthor());
        // The tip message must survive the merge-base walk that runs when comparing two branches.
        assertNotNull(status.lastCommitMessage());

        // The current branch compared with itself is neither ahead nor behind.
        BranchStatus self = repo.getBranchStatus(BASE, BASE);
        assertEquals(0, self.commitsAhead());
        assertEquals(0, self.commitsBehind());

        var statuses = repo.getBranchStatuses(List.of(BRANCH, SAME_TIP_BRANCH, BASE, "missing"), BASE);
        assertEquals(3, statuses.size());
        var branchStatus = statuses.get(BRANCH);
        assertNotNull(branchStatus);
        assertEquals(2, branchStatus.commitsAhead());
        assertEquals(1, branchStatus.commitsBehind());
        var sameTipStatus = statuses.get(SAME_TIP_BRANCH);
        assertNotNull(sameTipStatus);
        assertEquals(0, sameTipStatus.commitsAhead());
        assertEquals(0, sameTipStatus.commitsBehind());
        var baseStatus = statuses.get(BASE);
        assertNotNull(baseStatus);
        assertEquals(0, baseStatus.commitsAhead());
        assertEquals(0, baseStatus.commitsBehind());
    }

    private GitRepository createRepository(String remoteUri, File local, String repositoriesFolder) throws IOException {
        GitRepository newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        newRepo.setUri(remoteUri);
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        FileSystemRepository settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(local.getParent() + "/git-settings");
        String locksRoot = new File(root, "locks").getAbsolutePath();
        newRepo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        newRepo.setCommentTemplate("OpenL Studio: {commit-type}. {user-message}");
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, remoteUri, local, repositoriesFolder, true, true));
        return newRepo;
    }
}
