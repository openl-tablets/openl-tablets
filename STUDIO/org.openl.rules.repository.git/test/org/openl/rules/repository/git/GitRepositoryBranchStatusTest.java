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
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        try (var git = Git.init().setInitialBranch(BASE).setDirectory(template).call()) {
            var repository = git.getRepository();
            var parent = repository.getDirectory().getParentFile();
            var config = repository.getConfig();
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
        var remote = new File(root, "remote");
        var repositoriesFolder = new File(root, "repositories");
        var local = new File(repositoriesFolder, "local");
        FileUtils.copy(template, remote);
        repo = createRepository(remote.toURI().toString(), local, repositoriesFolder.getAbsolutePath());
    }

    @Test
    void readsLastCommitForEachBranch() throws IOException {
        var branch = repo.forBranch(BRANCH);
        branch.save(createFileData("rules/project1/file1", "one"), IOUtils.toInputStream("one"));
        branch.save(createFileData("rules/project1/file2", "two"), IOUtils.toInputStream("two"));
        repo.save(createFileData("rules/project1/file3", "three"), IOUtils.toInputStream("three"));
        repo.createBranch("rules/project1", SAME_TIP_BRANCH, BASE);

        var statuses = repo.getBranchStatuses(List.of(BRANCH, SAME_TIP_BRANCH, BASE, "missing"));
        // The unresolvable "missing" branch is omitted, the rest carry tip metadata.
        assertEquals(3, statuses.size());
        for (String branchName : List.of(BRANCH, SAME_TIP_BRANCH, BASE)) {
            var status = statuses.get(branchName);
            assertNotNull(status, branchName);
            assertNotNull(status.lastCommitRevision());
            assertNotNull(status.lastCommitAt());
            assertNotNull(status.lastCommitAuthor());
            assertNotNull(status.lastCommitMessage());
        }
    }

    private GitRepository createRepository(String remoteUri, File local, String repositoriesFolder) throws IOException {
        var newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        newRepo.setUri(remoteUri);
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        var settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(local.getParent() + "/git-settings");
        var locksRoot = new File(root, "locks").getAbsolutePath();
        newRepo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, remoteUri, local, repositoriesFolder, true, true));
        return newRepo;
    }
}
