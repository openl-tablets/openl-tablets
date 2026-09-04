package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.lib.PersonIdent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FileData;

/**
 * Git keeps commit time in whole seconds, so commits of one second have no order of their own. A commit must never be
 * reported ahead of its own descendant even then.
 */
class SameSecondHistoryOrderTest {

    private static final String REPO_ID = "design";
    private static final String PROJECT = "rules/project1";
    private static final String CREATED = "Project project1 is created.";
    private static final String SAVED_IN_TWO = "Save changes in branch two";
    private static final Instant SECOND = Instant.parse("2026-08-13T10:00:00Z");

    @TempDir
    private File root;
    @TempDir
    private Path localRepositoriesFolder;
    @AutoClose
    private @Nullable GitRepository repo;

    @Test
    void mergedRevisionIsCurrentWhenParentsShareASecond() throws Exception {
        repo = createRepository(buildMergedProject(SECOND.plusSeconds(1)), "one");

        assertEquals(List.of(CREATED, SAVED_IN_TWO), comments(repo.listHistory(PROJECT)));
        assertEquals(SAVED_IN_TWO, repo.check(PROJECT).getComment());
    }

    @Test
    void mergedRevisionIsCurrentWhenTheWholeMergeSharesASecond() throws Exception {
        repo = createRepository(buildMergedProject(SECOND), "one");

        assertEquals(List.of(CREATED, SAVED_IN_TWO), comments(repo.listHistory(PROJECT)));
        assertEquals(SAVED_IN_TWO, repo.check(PROJECT).getComment());
    }

    /**
     * Creates a project on {@code master}, adds a table on the branch {@code two} within the same second and merges
     * {@code two} into the branch {@code one}, which is still on the create commit.
     */
    private File buildMergedProject(Instant mergedAt) throws Exception {
        var local = new File(root, "design-repository");
        var author = new PersonIdent("Admin", "admin@email", SECOND, ZoneOffset.UTC);
        try (var git = Git.init().setDirectory(local).setInitialBranch("master").call()) {
            write(local, PROJECT + "/file1", "created");
            git.add().addFilepattern(".").call();
            git.commit().setMessage(CREATED).setAuthor(author).setCommitter(author).call();

            git.branchCreate().setName("one").call();
            git.branchCreate().setName("two").call();

            git.checkout().setName("two").call();
            write(local, PROJECT + "/file2", "a table added in the branch two");
            git.add().addFilepattern(".").call();
            git.commit().setMessage(SAVED_IN_TWO).setAuthor(author).setCommitter(author).call();

            git.checkout().setName("one").call();
            git.merge()
                    .include(git.getRepository().findRef("two"))
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                    .setCommit(false)
                    .call();
            var merger = new PersonIdent("Admin", "admin@email", mergedAt, ZoneOffset.UTC);
            git.commit().setMessage("Merge branch two into one").setAuthor(merger).setCommitter(merger).call();
        }
        return local;
    }

    private static List<String> comments(List<FileData> history) {
        return history.stream().map(FileData::getComment).toList();
    }

    private static void write(File local, String path, String text) throws IOException {
        var file = new File(local, path);
        Files.createDirectories(file.toPath().getParent());
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
    }

    private GitRepository createRepository(File local, String branch) throws IOException {
        var newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        var uri = local.getAbsolutePath();
        newRepo.setUri(uri);
        var repositoriesFolder = localRepositoriesFolder.toFile().getAbsolutePath();
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        newRepo.setBranch(branch);
        newRepo.setGcAutoDetach(false);
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, uri, local, repositoriesFolder, false, false));

        return newRepo;
    }
}
