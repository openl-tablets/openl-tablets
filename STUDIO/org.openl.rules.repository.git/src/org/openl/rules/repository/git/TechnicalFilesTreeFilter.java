package org.openl.rules.repository.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.PathPatterns;

/**
 * Leaves the technical files of a folder out of a tree walk: the ignored paths, an ignored folder with everything in
 * it, and the protected files of the project the folder is.
 *
 * <p>A protected file is told by its path relative to the folder: a walk over a project leaves its {@code pom.xml}
 * out, while a walk over a file of it leaves out the ignored paths only.
 */
@RequiredArgsConstructor
final class TechnicalFilesTreeFilter extends TreeFilter {

    private final IgnoredPaths ignoredPaths;
    private final PathPatterns protectedFiles;
    /** The folder walked over, ending with {@code /}. */
    private final String folder;

    @Override
    public boolean include(TreeWalk walker) {
        var path = walker.getPathString();
        return !isProtected(path) && !ignoredPaths.matches(path);
    }

    private boolean isProtected(String path) {
        return path.startsWith(folder) && protectedFiles.matches(path.substring(folder.length()));
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
