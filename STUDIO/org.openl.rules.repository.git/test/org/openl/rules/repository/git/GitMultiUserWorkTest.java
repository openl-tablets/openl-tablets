package org.openl.rules.repository.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openl.rules.repository.git.TestGitUtils.createFileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class GitMultiUserWorkTest {

    static final int MAX_THREADS = Math.min(6, Runtime.getRuntime().availableProcessors() * 2);
    private static final String FOLDER_IN_REPOSITORY = "rules/project";

    private Path root;
    private GitRepository repo;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory("openl");
        repo = createRepository(root.resolve("design-repository"));
    }

    @After
    public void tearDown() {
        if (repo != null) {
            repo.close();
        }
        FileUtils.deleteQuietly(root);
        if (Files.exists(root)) {
            fail("Cannot delete folder " + root);
        }
    }

    @Test
    public void simulateMultiUserWork() throws InterruptedException, IOException {
        createCommitAndCheck(repo, "README.md", "Initialize repository");
        AtomicBoolean passedStatus = new AtomicBoolean(true);
        CountDownLatch countDown = new CountDownLatch(MAX_THREADS);
        for (int i = 0; i < MAX_THREADS; i++) {
            final int idx = i;
            new Thread(() -> {
                GitRepository branchRepo = null;
                try {
                    String branchName = "branch" + idx;
                    String projectPath = FOLDER_IN_REPOSITORY + "_" + idx;
                    repo.createBranch(projectPath, branchName);
                    branchRepo = repo.forBranch(branchName);
                    for (int commitNo = 0; commitNo < 30; commitNo++) {
                        String path = projectPath + "/folder/file" + idx;
                        String text = String.format("File located in '%s'. Commit id: %s", path, commitNo);
                        createCommitAndCheck(branchRepo, path, text);
                        if ((commitNo + 1) % 10 == 0) {
                            repo.merge(branchName, new UserInfo("admin", "admin@email", "Admin"), null);
                            assertTrue("Branch must be merged to the master",
                                repo.isMergedInto(branchName, repo.getBranch()));
                        }
                    }
                    repo.deleteBranch(projectPath, branchName);
                    assertFalse("Branch must be removed", repo.getBranches(projectPath).contains(branchName));
                } catch (Error | Exception e) {
                    passedStatus.set(false);
                    e.printStackTrace();
                } finally {
                    countDown.countDown();
                }
            }).start();
        }
        countDown.await();
        assertTrue("Test was failed!", passedStatus.get());
    }

    private static void createCommitAndCheck(GitRepository repo, String path, String text) throws IOException {
        FileData result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));

        assertNotNull(result);
        assertEquals(path, result.getName());
        assertEquals("jsmith@email", result.getAuthor().getEmail());
        assertEquals("John Smith", result.getAuthor().getDisplayName());
        assertEquals("Comment for " + path, result.getComment());
        assertEquals(text.length(), result.getSize());
        assertNotNull(result.getModifiedAt());

        assertEquals(text, IOUtils.toStringAndClose(repo.read(path).getStream()));
    }

    private GitRepository createRepository(Path local) {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(local.toAbsolutePath().toString());
        FileSystemRepository settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(local.getParent() + "/git-settings");
        String locksRoot = root.resolve("locks").toAbsolutePath().toString();
        repo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.setGcAutoDetach(false);
        repo.initialize();

        return repo;
    }

}
