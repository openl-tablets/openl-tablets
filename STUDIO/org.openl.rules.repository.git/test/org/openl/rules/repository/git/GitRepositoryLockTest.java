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

import org.eclipse.jgit.errors.LockFailedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class GitRepositoryLockTest {

    private Path root;
    private Path repoRoot;
    private GitRepository repo;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory("openl");
        repoRoot = root.resolve("design-repository");
        repo = createRepository(repoRoot);
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
    public void testSaveFileWhenLocked() throws IOException {
        Path indexLock = createIndexLock();
        String path = "rules/project1/folder/file4";
        String text = "File located in " + path;
        try {
            createCommitAndCheck(repo, path, text);
            fail("Expected exception");
        } catch (IOException e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertTrue("LockFailedException expected", cause instanceof LockFailedException);
        }
        assertTrue("Index lock file mustn't be removed", Files.exists(indexLock));
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

    @Test
    public void testLockFileRemoveOnGitInit() throws IOException {
        repo.close();
        Path indexLock = createIndexLock();
        repo = createRepository(repoRoot);
        assertFalse("Index lock file must be removed", Files.exists(indexLock));
    }

    private Path createIndexLock() throws IOException {
        Path lockFile = repoRoot.resolve(".git/index.lock");
        Files.createFile(lockFile);
        assertTrue("Index lock file must be created!", Files.exists(lockFile));
        return lockFile;
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
