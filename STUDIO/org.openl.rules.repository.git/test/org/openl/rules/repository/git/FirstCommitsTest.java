package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FirstCommitsTest {

    @TempDir
    private File root;
    @AutoClose
    private Git git;
    @AutoClose
    private GitRepository repo;

    private File local;
    private RevCommit created;
    private RevCommit changedA;
    private RevCommit changedB;
    private RevCommit changedD;
    private ObjectId head;

    /**
     * Creates the folders {@code a}, {@code b}, {@code c}, {@code c/x} and {@code d}, changes {@code a}, changes
     * {@code b} on a feature branch and {@code d} on {@code master}, and merges the feature into {@code master}.
     */
    @BeforeEach
    void setUp() throws Exception {
        local = new File(root, "repository");
        git = Git.init().setDirectory(local).setInitialBranch("master").call();
        created = commit("created", "a/file", "b/file", "c/file", "c/x/file", "d/file");
        changedA = commit("changed a", "a/file");
        git.branchCreate().setName("feature").call();
        git.checkout().setName("feature").call();
        changedB = commit("changed b", "b/file");
        git.checkout().setName("master").call();
        changedD = commit("changed d", "d/file");
        var merge = git.merge().include(git.getRepository().resolve("feature")).setMessage("Merge feature").call();
        head = merge.getNewHead();

        repo = new GitRepository();
        repo.setId("design");
        var uri = local.getAbsolutePath();
        repo.setUri(uri);
        var repositoriesFolder = new File(root, "repositories").getAbsolutePath();
        repo.setLocalRepositoriesFolder(repositoriesFolder);
        repo.setGcAutoDetach(false);
        repo.initialize(TestGitUtils.mockGitRootFactory("design", uri, local, repositoriesFolder, false, false));
    }

    @Test
    void findsTheLatestChangeOfEveryPathInOneWalk() throws Exception {
        var commits = new FirstCommits(repo, head, "a");
        for (var path : List.of("b", "c", "c/x", "d", "d/", "absent")) {
            assertTrue(commits.join(head, path));
        }

        assertEquals(changedA, idOf(commits, "a"));
        // The merge keeps the folder of the feature, so the feature commit is its latest change
        assertEquals(changedB, idOf(commits, "b"));
        // A folder and a folder inside it are weighed apart
        assertEquals(created, idOf(commits, "c"));
        assertEquals(created, idOf(commits, "c/x"));
        assertEquals(changedD, idOf(commits, "d"));
        assertEquals(changedD, idOf(commits, "d/"));
        assertNull(commits.of(git, "absent"));
    }

    @Test
    void findsWhatAWalkOfOnePathFinds() throws Exception {
        var commits = new FirstCommits(repo, head, "a");
        commits.join(head, "b");
        commits.join(head, "c/x");

        for (var path : List.of("a", "b", "c/x")) {
            assertEquals(repo.findFirstCommit(git, head, path), idOf(commits, path));
        }
    }

    @Test
    void aPathJoinsOnlyTheWalkFromItsCommitBeforeTheWalkStarts() throws Exception {
        var commits = new FirstCommits(repo, head, "a");

        assertFalse(commits.join(changedA, "b"), "A path read from another commit has another history");
        assertTrue(commits.join(head, "b"));
        assertNotNull(commits.of(git, "a"));
        assertFalse(commits.join(head, "c"), "The walk has found the revisions already");
        assertEquals(changedB, idOf(commits, "b"));
    }

    @Test
    void eachPathWalksAloneWhenTheWalkForAllFails() throws Exception {
        var failing = spy(repo);
        doThrow(new IOException("A pack cannot be read."))
                .when(failing)
                .findFirstCommits(any(), any(), argThat(FirstCommitsTest::several));
        var commits = new FirstCommits(failing, head, "a");
        commits.join(head, "b");
        commits.join(head, "c");

        assertEquals(changedA, idOf(commits, "a"));
        assertEquals(changedB, idOf(commits, "b"));
        assertEquals(created, idOf(commits, "c"));
        // The walk for all of them is not repeated for every path
        verify(failing, times(1)).findFirstCommits(any(), any(), argThat(FirstCommitsTest::several));
    }

    @Test
    void aFolderReadAfterTheWalkFindsItsOwnRevision() throws Exception {
        var a = repo.check("a");
        var b = repo.check("b");
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(changedA.getName(), a.getVersion());

        var changedAgain = commit("changed a again", "a/file");
        var again = repo.check("a");
        var c = repo.check("c");

        assertNotNull(again);
        assertNotNull(c);
        assertEquals(changedAgain.getName(), again.getVersion());
        assertEquals(created.getName(), c.getVersion());
        assertEquals(changedB.getName(), b.getVersion());
        assertEquals(changedA.getName(), a.getVersion());
    }

    @Test
    void foldersOfAListingFindTheirRevisions() throws Exception {
        var versions = repo.listFolders("")
                .stream()
                .map(folder -> folder.getName() + " " + folder.getVersion())
                .sorted()
                .toList();

        assertEquals(List.of("a " + changedA.getName(),
                "b " + changedB.getName(),
                "c " + created.getName(),
                "d " + changedD.getName()), versions);
    }

    private static boolean several(Collection<String> paths) {
        return paths.size() > 1;
    }

    private ObjectId idOf(FirstCommits commits, String path) throws Exception {
        var revision = commits.of(git, path);
        assertNotNull(revision, path);
        return revision.id();
    }

    private RevCommit commit(String message, String... files) throws Exception {
        for (var file : files) {
            var target = new File(local, file);
            target.getParentFile().mkdirs();
            TestGitUtils.writeText(target, message);
        }
        git.add().addFilepattern(".").call();
        return git.commit().setMessage(message).call();
    }
}
