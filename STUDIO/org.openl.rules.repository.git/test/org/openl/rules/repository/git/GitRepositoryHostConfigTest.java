package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FileItem;
import org.openl.util.IOUtils;

/**
 * The git configuration of the host does not change the line breaks of the files: the host here sets
 * {@code core.autocrlf=true}, which turns CRLF into LF on a commit and LF into CRLF on a checkout.
 */
class GitRepositoryHostConfigTest {
    private static final String REPO_ID = "design";
    private static final String BRANCH = "master";
    private static final String PATH = "rules/project1/notes.txt";
    private static final String CRLF_TEXT = "Premium notes\r\nBase premium is 100.\r\n";
    private static final String LF_TEXT = "Premium notes\nBase premium is 100.\n";

    @TempDir
    private Path root;
    @AutoClose
    private GitRepository repo;
    private SystemReader hostReader;

    @BeforeEach
    void setUp() throws IOException {
        var userConfig = Files.writeString(root.resolve("gitconfig"), "[core]\n\tautocrlf = true\n");
        hostReader = SystemReader.getInstance();
        SystemReader.setInstance(new SystemReader.Delegate(hostReader) {
            @Override
            public FileBasedConfig openUserConfig(Config parent, FS fs) {
                return new FileBasedConfig(parent, userConfig.toFile(), fs);
            }
        });
    }

    @AfterEach
    void tearDown() {
        SystemReader.setInstance(hostReader);
    }

    @Test
    void keepsLineBreaksWhenHostConvertsThem() throws IOException {
        var local = root.resolve("design-repository");
        repo = createRepository(local.toString(), local, false, true);

        var saved = repo.save(createFileData(PATH, CRLF_TEXT), IOUtils.toInputStream(CRLF_TEXT));

        assertEquals(CRLF_TEXT, read(repo.read(PATH)));
        assertEquals(CRLF_TEXT, read(repo.readHistory(PATH, saved.getVersion())));
    }

    @Test
    void cloneChecksOutFilesAsStored() throws IOException, GitAPIException {
        var remote = createRemote();
        var local = root.resolve("repositories").resolve("clone");

        repo = createRepository(remote.toUri().toString(), local, true, true);

        assertEquals(LF_TEXT, Files.readString(local.resolve(PATH)));
    }

    @Test
    void copiesStoredFileOfCloneCheckedOutWithOtherLineBreaks() throws IOException, GitAPIException {
        var remote = createRemote();
        var local = root.resolve("repositories").resolve("clone");
        try (var git = Git.cloneRepository().setURI(remote.toUri().toString()).setDirectory(local.toFile()).call()) {
            // A clone of an older version, checked out with the line breaks of the host long before
            var file = local.resolve(PATH);
            assertEquals(CRLF_TEXT, Files.readString(file));
            var checkedOut = Instant.now().minus(1, ChronoUnit.HOURS);
            Files.setLastModifiedTime(file, FileTime.from(checkedOut));
            var index = git.getRepository().lockDirCache();
            index.getEntry(PATH).setLastModified(checkedOut);
            index.write();
            index.commit();
        }
        repo = createRepository(remote.toUri().toString(), local, true, false);

        var copy = "rules/project1/notes-copy.txt";
        repo.copyHistory(PATH, createFileData(copy, LF_TEXT), null);

        assertEquals(LF_TEXT, read(repo.read(copy)));
    }

    private Path createRemote() throws IOException, GitAPIException {
        var remote = root.resolve("remote");
        try (var git = Git.init().setDirectory(remote.toFile()).setInitialBranch(BRANCH).call()) {
            var file = remote.resolve(PATH);
            Files.createDirectories(file.getParent());
            Files.writeString(file, LF_TEXT);
            git.add().addFilepattern(PATH).call();
            git.commit().setMessage("Initial commit").setCommitter("User 1", "user1@email.to").call();
        }
        return remote;
    }

    private GitRepository createRepository(String uri, Path gitRoot, boolean remote, boolean empty) throws IOException {
        var repositoriesFolder = root.resolve("repositories").toString();
        var newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        newRepo.setUri(uri);
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        newRepo.setBranch(BRANCH);
        newRepo.setGcAutoDetach(false);
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID,
                uri,
                gitRoot.toFile(),
                repositoriesFolder,
                remote,
                empty));
        return newRepo;
    }

    private static String read(FileItem item) throws IOException {
        try (var stream = item.getStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
