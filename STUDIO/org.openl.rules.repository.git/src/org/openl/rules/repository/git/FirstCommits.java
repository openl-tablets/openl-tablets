package org.openl.rules.repository.git;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jspecify.annotations.Nullable;

/**
 * The latest commits that change the paths read from one commit, found together in one walk over the history.
 *
 * <p>A project listing reads the revision of every project it lists. Each project alone would walk the history back to
 * its last change, and a project left unchanged for a long time walks almost the whole history. One walk for all of
 * them reads each commit once.
 *
 * <p>Paths join until the revision of any of them is first asked for. That question walks the history for all the
 * paths joined so far. A path read later joins another walk.
 *
 * <p>When that walk fails, each path walks the history alone, as it did before it joined: a failure is told only for
 * the path it belongs to, and is not repeated for every path of the walk.
 */
@Slf4j
final class FirstCommits {

    /**
     * A commit as the revision of a file tells it. It keeps no other commit, as the parents of a commit would keep the
     * history they lead to.
     */
    record Revision(ObjectId id, PersonIdent committer, String message) {

        static Revision of(RevCommit commit) {
            return new Revision(commit.copy(), commit.getCommitterIdent(), commit.getFullMessage());
        }
    }

    private final GitRepository repository;
    private final ObjectId start;
    private final Set<String> paths = new HashSet<>();
    private boolean closed;
    private boolean failed;
    private @Nullable Map<String, Revision> revisions;

    /**
     * @param start the commit the walk starts from
     * @param path  the first path of the walk
     */
    FirstCommits(GitRepository repository, ObjectId start, String path) {
        this.repository = repository;
        this.start = start.copy();
        paths.add(path);
    }

    /**
     * Adds the path to the walk.
     *
     * @return {@code false} when the path is read from another commit, or the walk has started already
     */
    synchronized boolean join(ObjectId from, String path) {
        if (closed || !start.equals(from)) {
            return false;
        }
        paths.add(path);
        return true;
    }

    /**
     * The latest commit, from the start back, that changes the joined path, or {@code null} when none does.
     *
     * <p>The first question walks the history for every joined path. A question asked meanwhile waits for that walk.
     */
    @Nullable Revision of(Git git, String path) throws IOException, GitAPIException {
        var found = jointRevisions(git);
        if (found != null) {
            return found.get(path);
        }
        var commit = repository.findFirstCommit(git, start, path);
        return commit == null ? null : Revision.of(commit);
    }

    /**
     * The revisions of the joined paths, or {@code null} when the walk for all of them failed.
     */
    @Synchronized
    private @Nullable Map<String, Revision> jointRevisions(Git git) {
        if (revisions == null && !failed) {
            var joined = close();
            try {
                var found = new HashMap<String, Revision>();
                repository.findFirstCommits(git, start, joined)
                        .forEach((path, commit) -> found.put(path, Revision.of(commit)));
                revisions = found;
            } catch (IOException | GitAPIException | RuntimeException e) {
                failed = true;
                log.warn("Cannot find the revisions of {} paths in one walk, each is found alone: {}",
                        joined.size(),
                        e.getMessage(),
                        e);
            }
        }
        return revisions;
    }

    private synchronized Set<String> close() {
        closed = true;
        return Set.copyOf(paths);
    }
}
