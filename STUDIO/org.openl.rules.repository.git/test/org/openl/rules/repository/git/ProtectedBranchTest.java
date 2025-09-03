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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
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

public class ProtectedBranchTest {
    private static final String BRANCH1 = "branch1";
    @TempDir
    private static File template;
    @TempDir
    private File root;
    @AutoClose
    private GitRepository repo;

    @BeforeAll
    public static void initTest() throws GitAPIException, IOException {
        assumeSupportedPlatform();

        // Initialize remote repository
        try (Git git = Git.init().setDirectory(template).call()) {
            Repository repository = git.getRepository();
            File parent = repository.getDirectory().getParentFile();

            StoredConfig config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();

            createNewFile(parent, "file-in-master", "root");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial").setCommitter("user1", "user1@mail.to").call();

            git.branchCreate().setName(BRANCH1).call();
        }
    }

    @BeforeEach
    public void setUp() throws IOException {
        File remote = new File(root, "remote");
        File local = new File(root, "local");

        FileUtils.copy(template, remote);
        String remoteUri = remote.toURI().toString();

        // Clone remote repository and modify pre-push hook, then close it.
        try (GitRepository ignored = createRepository(remoteUri, local)) {
            writePrePushHook(local.getPath());
        }
        // Open the repository with a new pre-push hook
        repo = createRepository(remoteUri, local);
    }

    @Test
    public void cantSaveInMaster() {
        String path = "rules/project1/file1";
        String text = "File located in " + path;
        try {
            repo.save(createFileData(path, text), IOUtils.toInputStream(text));
            fail("The file shouldn't be committed in master branch");
        } catch (IOException e) {
            assertEquals("Rejected by \"pre-push\" hook.\n", e.getMessage());
        }
    }

    @Test
    public void rollBackMergeToMaster() throws IOException {
        GitRepository repoBranch1 = repo.forBranch(BRANCH1);

        // Make 2 commits
        final String path1 = "rules/project1/file1";
        repoBranch1.save(createFileData(path1, path1), IOUtils.toInputStream(path1));

        final String path2 = "rules/project1/file2";
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

    private GitRepository createRepository(String remoteUri, File local) {
        GitRepository repo = new GitRepository();
        repo.setUri(remoteUri);
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        FileSystemRepository settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(local.getParent() + "/git-settings");
        String locksRoot = new File(root, "locks").getAbsolutePath();
        repo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        repo.setCommentTemplate("OpenL Studio: {commit-type}. {user-message}");
        repo.initialize();

        return repo;
    }

    /**
     * Restrict push operation in 'master'
     */
    private void writePrePushHook(String parent) throws IOException {
        String hookScript = "#!/bin/bash\n" + "protected_branch='master'\n" + "current_branch=$(git symbolic-ref HEAD | sed -e 's,.*/\\(.*\\),\\1,')\n" + "if [ $protected_branch = $current_branch ]\n" + "then\n" + "    exit 1 # push will not execute\n" + "else\n" + "    exit 0 # push will execute\n" + "fi";

        File path = new File(parent + "/.git/hooks/", PrePushHook.NAME);
        writeText(path, hookScript);
        FS.DETECTED.setExecute(path, true);
    }

    private static void assumeSupportedPlatform() {
        assumeTrue(FS.DETECTED instanceof FS_POSIX || FS.DETECTED instanceof FS_Win32_Cygwin,
                "Hooks aren't supported on your platform");
    }

}
