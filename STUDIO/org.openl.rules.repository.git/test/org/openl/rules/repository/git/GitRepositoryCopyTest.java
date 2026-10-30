package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A copy of the latest version of a file keeps the mode the file is stored with.
 */
class GitRepositoryCopyTest {
    private static final String REPO_ID = "design";
    private static final String BRANCH = "master";
    private static final String PATH = "rules/project1/run.sh";
    private static final String COPY = "rules/project1/run-copy.sh";

    @TempDir
    private Path root;
    @AutoClose
    private GitRepository repo;

    @Test
    void keepsExecutableMode() throws IOException, GitAPIException {
        assumeTrue(FS.DETECTED.supportsExecute(), "The file system has no executable mode");
        var local = root.resolve("design-repository");
        try (var git = Git.init().setDirectory(local.toFile()).setInitialBranch(BRANCH).call()) {
            var file = local.resolve(PATH);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "echo run\n");
            FS.DETECTED.setExecute(file.toFile(), true);
            git.add().addFilepattern(PATH).call();
            git.commit().setMessage("Initial commit").setCommitter("User 1", "user1@email.to").call();
        }
        repo = createRepository(local);

        repo.copyHistory(PATH, createFileData(COPY, "echo run\n"), null);

        assertEquals(FileMode.EXECUTABLE_FILE, storedMode(local, COPY));
    }

    private GitRepository createRepository(Path local) throws IOException {
        var repositoriesFolder = root.resolve("repositories").toString();
        var newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        newRepo.setUri(local.toString());
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        newRepo.setBranch(BRANCH);
        newRepo.setGcAutoDetach(false);
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID,
                local.toString(),
                local.toFile(),
                repositoriesFolder,
                false,
                false));
        return newRepo;
    }

    private static FileMode storedMode(Path local, String path) throws IOException {
        try (var git = Git.open(local.toFile()); var walk = new RevWalk(git.getRepository())) {
            var commit = walk.parseCommit(git.getRepository().resolve(Constants.HEAD));
            try (var tree = TreeWalk.forPath(git.getRepository(), path, commit.getTree())) {
                return tree.getFileMode(0);
            }
        }
    }
}
