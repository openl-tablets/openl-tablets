package org.openl.rules.repository.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import org.openl.rules.repository.IgnoredPaths;

/**
 * Leaves the ignored paths out of a tree walk, an ignored folder with everything in it.
 */
@RequiredArgsConstructor
final class IgnoredPathsTreeFilter extends TreeFilter {

    private final IgnoredPaths ignoredPaths;

    @Override
    public boolean include(TreeWalk walker) {
        return !ignoredPaths.matches(walker.getPathString());
    }

    /**
     * An ignored file is told only by its own path, so the walk goes down to the files, even of a project at the root
     * of the repository that no other filter of the walk goes into.
     */
    @Override
    public boolean shouldBeRecursive() {
        return true;
    }

    // The filter keeps no state, so the walks share it: it is never copied.
    @SuppressWarnings({"java:S2975", "java:S1182"})
    @Override
    public TreeFilter clone() {
        return this;
    }
}
