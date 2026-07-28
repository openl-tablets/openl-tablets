package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.util.ZipUtils;

class GitRootFactoryTest {

    private static final String REMOTE_URI = "https://repo.example.com/fake.git";

    @TempDir
    private File localRepositoriesFolder;
    @TempDir
    private File localFolder;

    @Test
    void testURIIdentity() throws URISyntaxException {
        var a = new URIish("https://github.com/openl-tablets/openl-tablets.git");
        var b = new URIish("https://github.com/openl-tablets/openl-tablets.git");
        assertEquals(a, b);
        assertTrue(GitRootFactory.isSame(a, b));

        b = new URIish("http://github.com/openl-tablets/openl-tablets.git/");
        assertNotEquals(a, b);
        assertTrue(GitRootFactory.isSame(a, b));

        b = new URIish("http://github.com/openl-tablets/openl-tablets.git?a=foo&b=bar");
        assertNotEquals(a, b);
        assertFalse(GitRootFactory.isSame(a, b));
    }

    @Test
    void testLocalNew() throws IOException {
        var gitRootFactory = new GitRootFactory();

        var gitRoot = gitRootFactory.create("design", localFolder.getAbsolutePath(), localRepositoriesFolder.getAbsolutePath());

        assertFalse(gitRoot.remote());
        assertTrue(gitRoot.empty());
        assertEquals(localFolder, gitRoot.localGitRoot());
    }

    @Test
    void testLocalExisting() throws IOException {
        ZipUtils.extractAll(new File("target/test-classes/repositories/GitRootFactoryTest/local.zip"), localFolder);
        var gitRootFactory = new GitRootFactory();

        var gitRoot = gitRootFactory.create("design", localFolder.getAbsolutePath(), localRepositoriesFolder.getAbsolutePath());

        assertFalse(gitRoot.remote());
        assertFalse(gitRoot.empty());
        assertEquals(localFolder, gitRoot.localGitRoot());
    }

    @Test
    void testLocalExistingNotGitRepository() {
        var repositoryFolder = new File(localFolder, "local");
        var repositorySubFolder = new File(repositoryFolder, ".git");
        assertTrue(repositorySubFolder.mkdirs());

        var gitRootFactory = new GitRootFactory();

        assertThrows(IOException.class, () -> gitRootFactory.create("design", repositoryFolder.getAbsolutePath(), localRepositoriesFolder.getAbsolutePath()));
    }

    @Test
    void testLocalExistingNotAFolder() throws IOException {
        var repositoryFile = new File(localFolder, "local");
        Files.writeString(repositoryFile.toPath(), "test");

        var gitRootFactory = new GitRootFactory();

        assertThrows(IOException.class, () -> gitRootFactory.create("design", repositoryFile.getAbsolutePath(), localRepositoriesFolder.getAbsolutePath()));
    }

    @Test
    void testRemoteNew() throws IOException {
        var gitRootFactory = new GitRootFactory();

        var gitRoot = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());

        assertTrue(gitRoot.remote());
        assertTrue(gitRoot.empty());
        assertEquals(localRepositoriesFolder, gitRoot.localGitRoot().getParentFile());
    }

    @Test
    void testRemoteExisting() throws IOException {
        var gitRootFactory = new GitRootFactory();
        var gitRootEmpty = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());
        ZipUtils.extractAll(new File("target/test-classes/repositories/GitRootFactoryTest/remote.zip"), gitRootEmpty.localGitRoot());

        var gitRoot = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());

        assertTrue(gitRoot.remote());
        assertFalse(gitRoot.empty());
        assertEquals(localRepositoriesFolder, gitRoot.localGitRoot().getParentFile());
    }

    @Test
    void testRemoteExistingNotAGitRepository() throws IOException {
        var gitRootFactory = new GitRootFactory();
        var gitRootInitial = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());
        var subFolder = new File(gitRootInitial.localGitRoot(), "sub");
        assertTrue(subFolder.mkdirs());

        var gitRoot = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());

        assertTrue(gitRoot.remote());
        assertTrue(gitRoot.empty());
        assertEquals(localRepositoriesFolder, gitRoot.localGitRoot().getParentFile());
        assertNotEquals(gitRootInitial.localGitRoot(), gitRoot.localGitRoot());
    }

    @Test
    void testRemoteExistingNotAFolder() throws IOException {
        var gitRootFactory = new GitRootFactory();
        var gitRootInitial = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());
        Files.writeString(gitRootInitial.localGitRoot().toPath(), "test");

        var gitRoot = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());

        assertTrue(gitRoot.remote());
        assertTrue(gitRoot.empty());
        assertEquals(localRepositoriesFolder, gitRoot.localGitRoot().getParentFile());
        assertNotEquals(gitRootInitial.localGitRoot(), gitRoot.localGitRoot());
    }

    @Test
    void testUpdatedURI() throws IOException {
        var gitRootFactory = new GitRootFactory();
        var gitRootEmpty = gitRootFactory.create("design", REMOTE_URI, localRepositoriesFolder.getAbsolutePath());
        ZipUtils.extractAll(new File("target/test-classes/repositories/GitRootFactoryTest/remote.zip"), gitRootEmpty.localGitRoot());

        //Here a scheme is http instead of https
        var httpUrl = "http://repo.example.com/fake.git";
        var gitRoot = gitRootFactory.create("design", httpUrl, localRepositoriesFolder.getAbsolutePath());

        assertTrue(gitRoot.remote());
        assertFalse(gitRoot.empty());
        assertEquals(localRepositoriesFolder, gitRoot.localGitRoot().getParentFile());
        assertEquals(gitRootEmpty.localGitRoot(), gitRoot.localGitRoot());
        try (var repository = Git.open(gitRoot.localGitRoot()).getRepository()) {
            var remoteUrl = repository.getConfig()
                    .getString(ConfigConstants.CONFIG_REMOTE_SECTION,
                            Constants.DEFAULT_REMOTE_NAME,
                            ConfigConstants.CONFIG_KEY_URL);
            assertEquals(httpUrl, remoteUrl);
        }
    }

    @Test
    void testDomainIsNotCaseSensitive() throws IOException {
        var httpUrlLower = "http://repo.example.com/fake.git";
        var httpUrlMixedCase = "http://Repo.Example.Com/fake.git";
        var gitRootFactory = new GitRootFactory();

        var gitRootLowerCase = gitRootFactory.create("design", httpUrlLower, localRepositoriesFolder.getAbsolutePath());
        var gitRootMixedCase = gitRootFactory.create("design", httpUrlMixedCase, localRepositoriesFolder.getAbsolutePath());

        assertEquals(gitRootMixedCase.localGitRoot(), gitRootLowerCase.localGitRoot());
    }

    @Test
    void testCollision() throws IOException {
        var gitRootFactory = new GitRootFactory();
        var httpUrl = "https://different-repo.example.com/fake.git";
        var gitRootEmpty = gitRootFactory.create("design", httpUrl, localRepositoriesFolder.getAbsolutePath());
        //Collision is modelled here since remote.zip has a different URL than in httpUrl
        ZipUtils.extractAll(new File("target/test-classes/repositories/GitRootFactoryTest/remote.zip"), gitRootEmpty.localGitRoot());

        var gitRoot = gitRootFactory.create("design", httpUrl, localRepositoriesFolder.getAbsolutePath());

        assertTrue(gitRoot.remote());
        assertTrue(gitRoot.empty());
        assertEquals(localRepositoriesFolder, gitRoot.localGitRoot().getParentFile());
        assertNotEquals(gitRootEmpty.localGitRoot(), gitRoot.localGitRoot());
    }
}
