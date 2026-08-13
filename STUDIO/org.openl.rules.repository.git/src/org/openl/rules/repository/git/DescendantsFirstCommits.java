package org.openl.rules.repository.git;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jspecify.annotations.Nullable;

/**
 * Reorders a commit walk so that a commit is never reported ahead of its own descendant.
 *
 * <p>
 * Git keeps commit time in whole seconds. Commits made within one second carry the same time, so a walk ordered by
 * commit time has nothing to tell them apart and may report a parent before its child. It happens on a merge target
 * branch, where the parents of a merge commit are reported one after another.
 *
 * <p>
 * Commits of one second are collected into a run and the run is reported descendants first. Commits of different
 * seconds keep the order of the underlying walk. Only one run is held at a time, so a long history is read a second
 * at a time rather than whole. A second normally holds a few commits, but a history written in one second, by a bulk
 * import for example, forms a single run and is read whole before its first commit is reported.
 *
 * <p>
 * A run is ordered by the parent links inside it. Two commits related only through a commit missing from the run, as
 * a filtering walk may leave it out, keep the order of the walk.
 *
 * <p>
 * The underlying walk is read once, so the iterator cannot be restarted.
 */
final class DescendantsFirstCommits implements Iterator<RevCommit> {

    private final Iterator<RevCommit> commits;
    private final Deque<RevCommit> ordered = new ArrayDeque<>();

    /**
     * The first commit of the next run, already read from the walk but not reported yet.
     */
    private @Nullable RevCommit nextRunStart;

    DescendantsFirstCommits(Iterator<RevCommit> commits) {
        this.commits = commits;
    }

    @Override
    public boolean hasNext() {
        fill();
        return !ordered.isEmpty();
    }

    @Override
    public RevCommit next() {
        fill();
        var next = ordered.poll();
        if (next == null) {
            throw new NoSuchElementException();
        }
        return next;
    }

    private void fill() {
        if (ordered.isEmpty()) {
            reportDescendantsFirst(readRun());
        }
    }

    /**
     * Reads the commits of the next second from the walk.
     */
    private List<RevCommit> readRun() {
        var first = nextRunStart;
        nextRunStart = null;
        if (first == null && commits.hasNext()) {
            first = commits.next();
        }
        var run = new ArrayList<RevCommit>();
        if (first == null) {
            return run;
        }
        run.add(first);
        while (commits.hasNext() && nextRunStart == null) {
            var commit = commits.next();
            if (commit.getCommitTime() == first.getCommitTime()) {
                run.add(commit);
            } else {
                nextRunStart = commit;
            }
        }
        return run;
    }

    /**
     * Reports the run so that every commit comes after all its descendants within the run.
     *
     * <p>
     * A commit is moved only when a descendant of it holds it back. The commit ready the earliest in the walk is always
     * reported first, so the run stays as close to the order of the walk as the descendants-first rule allows.
     */
    private void reportDescendantsFirst(List<RevCommit> run) {
        if (run.size() < 2) {
            ordered.addAll(run);
            return;
        }
        Map<RevCommit, Integer> positions = new HashMap<>();
        for (var i = 0; i < run.size(); i++) {
            positions.put(run.get(i), i);
        }
        var descendants = new int[run.size()];
        for (var commit : run) {
            for (var parent : commit.getParents()) {
                var position = positions.get(parent);
                if (position != null) {
                    descendants[position]++;
                }
            }
        }

        var ready = new PriorityQueue<Integer>();
        for (var i = 0; i < run.size(); i++) {
            if (descendants[i] == 0) {
                ready.add(i);
            }
        }
        while (!ready.isEmpty()) {
            var commit = run.get(ready.poll());
            ordered.add(commit);
            for (var parent : commit.getParents()) {
                var position = positions.get(parent);
                if (position != null && --descendants[position] == 0) {
                    ready.add(position);
                }
            }
        }
    }
}
