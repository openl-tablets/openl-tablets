package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.openl.rules.repository.git.TestGitUtils.createNewFile;
import static org.openl.rules.repository.git.TestGitUtils.writeText;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IllegalTodoFileModification;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;

public class GitRepositoryForcePullTest {

    private static final UserInfo USER_INFO = new UserInfo("jsmith", "jsmith@email", "John Smith");

    @TempDir
    private static File template;

    @TempDir
    private File root;
    @AutoClose
    private GitRepository repo;
    @AutoClose
    private Git local2;

    @BeforeAll
    static void initialize() throws IOException, GitAPIException {
        // Initialize remote repository
        try (Git git = Git.init().setDirectory(template).call()) {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();

            File parent = repository.getDirectory().getParentFile();

            // create initial commit in master
            createNewFile(parent, "README.md", "# openl-merge-template");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit")
                    .setCommitter("User 1", "user1@email.to")
                    .call();
        }
    }

    @BeforeEach
    void setUp() throws IOException, GitAPIException {
        File remote = new File(root, "remote");
        File local = new File(root, "local");

        FileUtils.copy(template, remote);
        repo = createRepository(remote, local);

        this.local2 = Git.cloneRepository()
                .setURI(remote.toURI().toString())
                .setDirectory(new File(root, "local-2"))
                .call();
    }

    private GitRepository createRepository(File remote, File local) {
        GitRepository repo = new GitRepository();
        repo.setUri(remote.toURI().toString());
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setBranch("master");
        repo.setCommentTemplate("OpenL Studio: {commit-type}. {user-message}");
        String settingsPath = local.getParent() + "/git-settings";
        FileSystemRepository settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(settingsPath);
        String locksRoot = new File(local.getParent(), "locks").getAbsolutePath();
        repo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        repo.setGcAutoDetach(false);
        repo.initialize();

        return repo;
    }

    @Test
    void when_amend_commit_and_force_push_history_is_rewritten() throws IOException, GitAPIException {
        // ─── Step 1: Capture original commit ────────────────────────────────
        var oldHistory = repo.listHistory("README.md");
        assertEquals(1, oldHistory.size(), "Expected initial commit history");
        var originalCommit = oldHistory.get(0);
        assertEquals("Initial commit", originalCommit.getComment());
        var originalCommitId = originalCommit.getVersion();

        // ─── Step 2: Amend commit and force push ────────────────────────────
        File workDir = local2.getRepository().getDirectory().getParentFile();
        createNewFile(workDir, "test.txt", "Lorem ipsum dolor sit amet");

        local2.add().addFilepattern(".").call();
        local2.commit()
                .setAmend(true)
                .setMessage("New amended commit message")
                .call();

        ObjectId amendedCommitId = local2.getRepository().resolve("HEAD");

        local2.push()
                .setForce(true)
                .call();

        // ─── Step 3: Pull changes into target repo ──────────────────────────
        repo.pull(USER_INFO);

        // ─── Step 4: Verify updated commit ──────────────────────────────────
        var updatedHistory = repo.listHistory("README.md");
        assertEquals(1, updatedHistory.size(), "Expected same single entry after amend");

        var amendedCommit = updatedHistory.get(0);
        assertNotEquals(originalCommitId, amendedCommit.getVersion(), "Commit ID should have changed after amend");
        assertEquals("New amended commit message", amendedCommit.getComment(), "Amended commit message mismatch");
        assertEquals(amendedCommitId.getName(), amendedCommit.getVersion(), "Expected HEAD to match amended commit");

        // ─── Step 5: Verify file content from amended commit ────────────────
        var testTxtContent = new String(repo.read("test.txt").getStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("Lorem ipsum dolor sit amet", testTxtContent, "test.txt content mismatch");
    }

    @Test
    void when_commits_squashed_and_force_pushed_then_history_is_rewritten() throws Exception {
        // ─── Step 1: Create initial commit in local2 ────────────────────────
        File repoDir = local2.getRepository().getDirectory().getParentFile();
        createNewFile(repoDir, "test.txt", "First file");
        local2.add().addFilepattern("test.txt").call();
        local2.commit().setMessage("First commit").call();

        // ─── Step 2: Create second commit in local2 ─────────────────────────
        writeText(new File(repoDir, "test.txt"), "Second update");
        local2.add().addFilepattern("test.txt").call();
        local2.commit().setMessage("Second commit").call();

        // ─── Step 3: Push both commits to remote ────────────────────────────
        local2.push().call();

        // ─── Step 4: Pull from repo to sync remote ──────────────────────────
        repo.pull(USER_INFO);

        var fullHistoryBeforeSquash = repo.listHistory("test.txt");
        assertEquals(2, fullHistoryBeforeSquash.size(), "Expected 2 commits before squash");
        String originalSecondCommitId = fullHistoryBeforeSquash.get(0).getVersion();

        // ─── Step 5: Squash commits in local2 ───────────────────────────────
        ObjectId firstCommitId = local2.getRepository().resolve("HEAD~2");

        local2.rebase()
                .setUpstream(firstCommitId)
                .runInteractively(new RebaseCommand.InteractiveHandler() {
                    @Override
                    public void prepareSteps(List<RebaseTodoLine> steps) {
                        assertEquals(2, steps.size(), "Expected two commits to rebase");
                        for (int i = 1; i < steps.size(); i++) {
                            try {
                                steps.get(i).setAction(RebaseTodoLine.Action.SQUASH);
                            } catch (IllegalTodoFileModification e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    @Override
                    public String modifyCommitMessage(String oldMessage) {
                        return "Squashed commit";
                    }
                })
                .call();

        // ─── Step 6: Force push the squashed commit ─────────────────────────
        local2.push()
                .setForce(true)
                .call();

        ObjectId squashedCommitId = local2.getRepository().resolve("HEAD");

        // ─── Step 7: Pull again into repo ───────────────────────────────────
        repo.pull(USER_INFO);
        var historyAfterSquash = repo.listHistory("test.txt");

        // ─── Step 8: Assertions ─────────────────────────────────────────────
        assertEquals(1, historyAfterSquash.size(), "History should be squashed to 1 commit");
        assertEquals("Squashed commit", historyAfterSquash.get(0).getComment(), "Message mismatch");
        assertEquals(squashedCommitId.getName(), historyAfterSquash.get(0).getVersion(), "Commit ID mismatch");
        assertNotEquals(originalSecondCommitId, historyAfterSquash.get(0).getVersion(), "Commit ID should have changed");

        // ─── Step 9: Validate file content ──────────────────────────────────
        String firstContent = new String(repo.read("test.txt").getStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("Second update", firstContent);
    }

    @Test
    void when_commits_removed_and_force_pushed_then_history_is_rewritten() throws Exception {
        // ─── Step 1: Create 3 commits in local2 ─────────────────────────────
        File workDir = local2.getRepository().getDirectory().getParentFile();

        var subDir = new File(workDir, "subdir");
        subDir.mkdir();
        createNewFile(subDir, "a.txt", "A");
        local2.add().addFilepattern("subdir/a.txt").call();
        RevCommit commitA = local2.commit().setMessage("Commit A").call();

        createNewFile(subDir, "b.txt", "B");
        local2.add().addFilepattern("subdir/b.txt").call();
        RevCommit commitB = local2.commit().setMessage("Commit B").call();

        createNewFile(subDir, "c.txt", "C");
        local2.add().addFilepattern("subdir/c.txt").call();
        RevCommit commitC = local2.commit().setMessage("Commit C").call();

        // ─── Step 2: Push all commits ───────────────────────────────────────
        local2.push().call();

        // ─── Step 3: Pull into repo and verify 3 commits exist ──────────────
        repo.pull(USER_INFO);
        var historyBefore = repo.listHistory("subdir");
        assertEquals(3, historyBefore.size(), "Expected 3 commits before deletion");

        // ─── Step 4: Reset local2 to commit A ───────────────────────────────
        local2.reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef(commitA.getName())
                .call();

        // ─── Step 5: Force push reset history ───────────────────────────────
        local2.push()
                .setForce(true)
                .setRefSpecs(new RefSpec("refs/heads/master:refs/heads/master"))
                .call();

        // ─── Step 6: Pull again into repo ───────────────────────────────────
        repo.pull(USER_INFO);
        var historyAfter = repo.listHistory("subdir");

        // ─── Step 7: Verify only Commit A remains ───────────────────────────
        assertEquals(1, historyAfter.size(), "Only one commit should remain after force push");
        assertEquals("Commit A", historyAfter.get(0).getComment(), "Remaining commit should be A");

        // ─── Step 8: Ensure B and C are gone ────────────────────────────────
        assertNotEquals(commitB.getName(), historyAfter.get(0).getVersion(), "Commit B should be gone");
        assertNotEquals(commitC.getName(), historyAfter.get(0).getVersion(), "Commit C should be gone");

        // ─── Step 9: Validate content of a.txt only ─────────────────────────
        String contentA = new String(repo.read("subdir/a.txt").getStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("A", contentA);
    }

}
