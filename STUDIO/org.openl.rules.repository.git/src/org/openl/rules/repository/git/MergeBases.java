package org.openl.rules.repository.git;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jspecify.annotations.Nullable;

/**
 * The merge bases of the merge commits of one repository, each found once.
 *
 * <p>The history of a path weighs a merge commit against the commit its parents parted at. That commit depends on the
 * parents alone, never on the path. So the merge base found for the history of one project serves the history of every
 * other project. A commit never changes, so a merge base once found stays true.
 *
 * <p>The merge bases of at most {@value #LIMIT} merge commits are kept. Past that, all of them are forgotten.
 */
final class MergeBases {

    private static final int LIMIT = 100_000;

    /**
     * A merge base: the commit and its tree.
     */
    record MergeBase(ObjectId commit, ObjectId tree) {
    }

    /**
     * Stands for a merge commit whose parents have no history in common.
     */
    private static final MergeBase NONE = new MergeBase(ObjectId.zeroId(), ObjectId.zeroId());

    private final Map<ObjectId, MergeBase> found = new ConcurrentHashMap<>();

    /**
     * The commit the parents of the merge commit parted at, or {@code null} when their histories have none in common.
     * Of several such commits, as a criss-cross history has, the first one is taken.
     *
     * @param walk the walk to find it with when it is not known yet; it is reset, so it must serve no other walk
     */
    @Nullable MergeBase of(RevWalk walk, RevCommit merge) throws IOException {
        var known = found.get(merge);
        if (known == null) {
            var base = GitRepository.mergeBaseOf(walk, merge.getParents());
            known = base == null ? NONE : new MergeBase(base.copy(), base.getTree().copy());
            if (found.size() >= LIMIT) {
                found.clear();
            }
            found.put(merge.copy(), known);
        }
        return known == NONE ? null : known;
    }
}
