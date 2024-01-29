package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.errors.LockFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class GitRepositoryLockTest {

    private Path root;
    private Path repoRoot;
    private GitRepository repo;

    @BeforeEach
    public void setUp() throws IOException {
        root = Files.createTempDirectory("openl");
        repoRoot = root.resolve("design-repository");
        repo = createRepository(repoRoot);
    }

    @AfterEach
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
            assertTrue(cause instanceof LockFailedException, "LockFailedException expected");
        }
        assertTrue(Files.exists(indexLock), "Index lock file mustn't be removed");
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

        assertEquals(text, GitRepositoryTest.readText(repo.read(path)));
    }

    @Test
    public void testLockFileRemoveOnGitInit() throws IOException {
        repo.close();
        Path indexLock = createIndexLock();
        repo = createRepository(repoRoot);
        assertFalse(Files.exists(indexLock), "Index lock file must be removed");
    }

    private Path createIndexLock() throws IOException {
        Path lockFile = repoRoot.resolve(".git/index.lock");
        Files.createFile(lockFile);
        assertTrue(Files.exists(lockFile), "Index lock file must be created!");
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
