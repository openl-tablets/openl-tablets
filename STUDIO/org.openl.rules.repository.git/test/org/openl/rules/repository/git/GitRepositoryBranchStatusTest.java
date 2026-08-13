package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;
import static org.openl.rules.repository.git.TestGitUtils.createNewFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

class GitRepositoryBranchStatusTest {

    private static final String BASE = "master";
    private static final String BRANCH = "branch1";
    private static final String SAME_TIP_BRANCH = "same-tip";
    private static final String REPO_ID = "design";

    @TempDir
    private static File template;
    @TempDir
    private File root;
    @AutoClose
    private GitRepository repo;

    @BeforeAll
    static void initTemplate() throws GitAPIException, IOException {
        try (var git = Git.init().setInitialBranch(BASE).setDirectory(template).call()) {
            var repository = git.getRepository();
            var parent = repository.getDirectory().getParentFile();
            var config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();
            createNewFile(parent, "file-in-master", "root");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial").setCommitter("user1", "user1@mail.to").call();
            git.branchCreate().setName(BRANCH).call();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        var remote = new File(root, "remote");
        var repositoriesFolder = new File(root, "repositories");
        var local = new File(repositoriesFolder, "local");
        FileUtils.copy(template, remote);
        repo = createRepository(remote.toURI().toString(), local, repositoriesFolder.getAbsolutePath());
    }

    @Test
    void readsLastCommitForEachBranch() throws IOException {
        var branch = repo.forBranch(BRANCH);
        branch.save(createFileData("rules/project1/file1", "one"), IOUtils.toInputStream("one"));
        branch.save(createFileData("rules/project1/file2", "two"), IOUtils.toInputStream("two"));
        repo.save(createFileData("rules/project1/file3", "three"), IOUtils.toInputStream("three"));
        repo.createRepositoryBranch(SAME_TIP_BRANCH, BASE);

        var statuses = repo.getBranchStatuses(List.of(BRANCH, SAME_TIP_BRANCH, BASE, "missing"));
        // The unresolvable "missing" branch is omitted, the rest carry tip metadata.
        assertEquals(3, statuses.size());
        for (String branchName : List.of(BRANCH, SAME_TIP_BRANCH, BASE)) {
            var status = statuses.get(branchName);
            assertNotNull(status, branchName);
            assertNotNull(status.lastCommitRevision());
            assertNotNull(status.lastCommitAt());
            assertNotNull(status.lastCommitAuthor());
            assertNotNull(status.lastCommitMessage());
        }
    }

    @Test
    void reportsWhichBranchesTheConfigurationProtects() throws IOException {
        repo.createRepositoryBranch(SAME_TIP_BRANCH, BASE);
        repo.setProtectedBranches(BASE, "same-*");

        var statuses = repo.getBranchStatuses(List.of(BASE, BRANCH, SAME_TIP_BRANCH));

        assertTrue(Objects.requireNonNull(statuses.get(BASE)).protectedBranch());
        assertTrue(Objects.requireNonNull(statuses.get(SAME_TIP_BRANCH)).protectedBranch(),
                "A branch matching a configured pattern is protected.");
        assertFalse(Objects.requireNonNull(statuses.get(BRANCH)).protectedBranch());
    }

    @Test
    void resolvesTreeRevisionsAndPreservesMissingAndUnresolvedStates() throws IOException {
        var branch = repo.forBranch(BRANCH);
        branch.save(createFileData("rules/project1/rules.xml", "one"), IOUtils.toInputStream("one"));

        var revisions = repo.getBranchTreeRevisions(List.of(BASE, BRANCH, "missing"), "rules/");

        assertEquals(2, revisions.size());
        var baseRevision = Objects.requireNonNull(revisions.get(BASE));
        var branchRevision = Objects.requireNonNull(revisions.get(BRANCH));
        assertNull(baseRevision.treeRevision(), "The resolved base branch has no rules folder");
        assertNotNull(branchRevision.treeRevision());
        assertTrue(branchRevision.tipAffectsPath());
        assertFalse(revisions.containsKey("missing"), "An unresolved branch must be omitted");
    }

    @Test
    void keepsTreeRevisionWhenCommitChangesOutsideDiscoveryPath() throws IOException {
        var branch = repo.forBranch(BRANCH);
        branch.save(createFileData("rules/project1/rules.xml", "one"), IOUtils.toInputStream("one"));
        var before = Objects.requireNonNull(repo.getBranchTreeRevisions(List.of(BRANCH), "rules").get(BRANCH));

        branch.save(createFileData("outside.txt", "two"), IOUtils.toInputStream("two"));
        var after = Objects.requireNonNull(repo.getBranchTreeRevisions(List.of(BRANCH), "rules").get(BRANCH));

        assertNotEquals(before.branchRevision(), after.branchRevision());
        assertEquals(before.treeRevision(), after.treeRevision());
        assertFalse(after.tipAffectsPath());
    }

    @Test
    void createsAndDeletesRepositoryBranchWithoutProjectMetadata() throws IOException {
        var newBranch = "repository-only";

        repo.createRepositoryBranch(newBranch, BASE);

        assertTrue(repo.listBranches().contains(newBranch));
        assertEquals(Set.of(BASE, BRANCH, newBranch), Set.copyOf(repo.listBranches()));
        repo.deleteRepositoryBranch(newBranch);
        assertFalse(repo.listBranches().contains(newBranch));
    }

    @Test
    void rejectsBaseBranchDeletionRegardlessOfCasing() {
        assertThrows(IOException.class, () -> repo.deleteRepositoryBranch(BASE.toUpperCase(Locale.ROOT)));
    }

    private GitRepository createRepository(String remoteUri, File local, String repositoriesFolder) throws IOException {
        var newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        newRepo.setUri(remoteUri);
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, remoteUri, local, repositoriesFolder, true, true));
        return newRepo;
    }
}
