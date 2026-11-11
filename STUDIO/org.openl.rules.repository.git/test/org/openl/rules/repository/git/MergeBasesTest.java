package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MergeBasesTest {

    @TempDir
    private File root;
    @AutoClose
    private Git git;

    private final MergeBases mergeBases = new MergeBases();
    private File local;
    private RevCommit created;
    private RevCommit merged;

    /**
     * Creates a project on {@code master}, changes it on the branches {@code one} and {@code two}, and merges
     * {@code two} into {@code one}.
     */
    @BeforeEach
    void setUp() throws Exception {
        local = new File(root, "repository");
        git = Git.init().setDirectory(local).setInitialBranch("master").call();
        created = commit("file1", "created");
        git.branchCreate().setName("one").call();
        git.branchCreate().setName("two").call();

        git.checkout().setName("two").call();
        commit("file2", "changed in two");
        git.checkout().setName("one").call();
        commit("file3", "changed in one");
        var result = git.merge().include(git.getRepository().resolve("two")).setMessage("Merge two").call();
        merged = git.getRepository().parseCommit(result.getNewHead());
        assertEquals(2, merged.getParentCount());
    }

    @Test
    void findsTheCommitTheParentsPartedAt() throws IOException {
        try (var walk = new RevWalk(git.getRepository())) {
            var base = mergeBases.of(walk, merged);

            assertNotNull(base);
            assertEquals(created.getId(), base.commit());
            assertEquals(created.getTree().getId(), base.tree());
        }
    }

    @Test
    void answersAgainWithoutLookingTheCommitsUp() throws Exception {
        try (var walk = new RevWalk(git.getRepository())) {
            mergeBases.of(walk, merged);
        }
        // A walk over another repository knows none of the commits: only the merge base already found answers.
        try (var other = Git.init().setDirectory(new File(root, "other")).call();
             var walk = new RevWalk(other.getRepository())) {
            var base = mergeBases.of(walk, merged);

            assertNotNull(base);
            assertEquals(created.getId(), base.commit());
        }
    }

    @Test
    void unrelatedHistoriesHaveNone() throws Exception {
        var repository = git.getRepository();
        ObjectId merge;
        try (var inserter = repository.newObjectInserter()) {
            var unrelated = insert(inserter, "Unrelated");
            merge = insert(inserter, "Merge unrelated", created, unrelated);
            inserter.flush();
        }
        var commit = repository.parseCommit(merge);
        try (var walk = new RevWalk(repository)) {
            assertNull(mergeBases.of(walk, commit));
        }
        // The answer that there is none is kept as well.
        try (var other = Git.init().setDirectory(new File(root, "other")).call();
             var walk = new RevWalk(other.getRepository())) {
            assertNull(mergeBases.of(walk, commit));
        }
    }

    private RevCommit commit(String file, String content) throws Exception {
        TestGitUtils.writeText(new File(local, file), content);
        git.add().addFilepattern(".").call();
        return git.commit().setMessage(content).call();
    }

    /**
     * Writes a commit of the tree the project was created with.
     */
    private ObjectId insert(ObjectInserter inserter, String message, ObjectId... parents) throws IOException {
        var person = new PersonIdent("Admin", "admin@email");
        var builder = new CommitBuilder();
        builder.setTreeId(created.getTree());
        builder.setParentIds(parents);
        builder.setAuthor(person);
        builder.setCommitter(person);
        builder.setMessage(message);
        return inserter.insert(builder);
    }
}
