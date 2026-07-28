package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;
import static org.openl.rules.repository.git.TestGitUtils.createNewFile;
import static org.openl.rules.repository.git.TestGitUtils.writeText;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.hooks.PrePushHook;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS_POSIX;
import org.eclipse.jgit.util.FS_Win32_Cygwin;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

class ProtectedBranchTest {
    private static final String BRANCH1 = "branch1";
    private static final String REPO_ID = "design";
    @TempDir
    private static File template;
    @TempDir
    private File root;
    @AutoClose
    private GitRepository repo;

    @BeforeAll
    static void initTest() throws GitAPIException, IOException {
        assumeSupportedPlatform();

        // Initialize remote repository
        try (var git = Git.init().setDirectory(template).call()) {
            var repository = git.getRepository();
            var parent = repository.getDirectory().getParentFile();

            var config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();

            createNewFile(parent, "file-in-master", "root");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial").setCommitter("user1", "user1@mail.to").call();

            git.branchCreate().setName(BRANCH1).call();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        var remote = new File(root, "remote");
        var repositoriesFolder = new File(root, "repositories");
        var local = new File(repositoriesFolder, "local");

        FileUtils.copy(template, remote);
        var remoteUri = remote.toURI().toString();

        // Clone remote repository and modify pre-push hook, then close it.
        try (var ignored = createRepository(remoteUri, local, repositoriesFolder.getAbsolutePath(), true)) {
            writePrePushHook(local.getPath());
        }
        // Open the repository with a new pre-push hook
        repo = createRepository(remoteUri, local, repositoriesFolder.getAbsolutePath(), false);
    }

    @Test
    void cantSaveInMaster() {
        var path = "rules/project1/file1";
        var text = "File located in " + path;
        try {
            repo.save(createFileData(path, text), IOUtils.toInputStream(text));
            fail("The file shouldn't be committed in master branch");
        } catch (IOException e) {
            assertEquals("Rejected by \"pre-push\" hook.\n", e.getMessage());
        }
    }

    @Test
    void rollBackMergeToMaster() throws IOException {
        var repoBranch1 = repo.forBranch(BRANCH1);

        // Make 2 commits
        final var path1 = "rules/project1/file1";
        repoBranch1.save(createFileData(path1, path1), IOUtils.toInputStream(path1));

        final var path2 = "rules/project1/file2";
        repoBranch1.save(createFileData(path2, path2), IOUtils.toInputStream(path2));

        try {
            repo.merge(BRANCH1, new UserInfo("john", "john@email", "John"), null);
            fail("Merge must be unavailable because of pre-push hook");
        } catch (IOException e) {
            // After merge failure must rollback both commits from 'branch1'.
            assertNull(repo.check(path1),
                    "The file " + path1 + " must be absent in 'master' after rolling back merge.");
            assertNull(repo.check(path2),
                    "The file " + path2 + " must be absent in 'master' after rolling back merge.");
        }
    }

    private GitRepository createRepository(String remoteUri, File local, String repositoriesFolder, boolean empty) throws IOException {
        var newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        newRepo.setUri(remoteUri);
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        var settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(local.getParent() + "/git-settings");
        var locksRoot = new File(root, "locks").getAbsolutePath();
        newRepo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        newRepo.setCommentTemplate("OpenL Studio: {commit-type}. {user-message}");
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, remoteUri, local, repositoriesFolder, true, empty));

        return newRepo;
    }

    /**
     * Restrict push operation in 'master'
     */
    private void writePrePushHook(String parent) throws IOException {
        var hookScript = "#!/bin/bash\n" + "protected_branch='master'\n" + "current_branch=$(git symbolic-ref HEAD | sed -e 's,.*/\\(.*\\),\\1,')\n" + "if [ $protected_branch = $current_branch ]\n" + "then\n" + "    exit 1 # push will not execute\n" + "else\n" + "    exit 0 # push will execute\n" + "fi";

        var path = new File(parent + "/.git/hooks/", PrePushHook.NAME);
        writeText(path, hookScript);
        FS.DETECTED.setExecute(path, true);
    }

    private static void assumeSupportedPlatform() {
        assumeTrue(FS.DETECTED instanceof FS_POSIX || FS.DETECTED instanceof FS_Win32_Cygwin,
                "Hooks aren't supported on your platform");
    }

}
