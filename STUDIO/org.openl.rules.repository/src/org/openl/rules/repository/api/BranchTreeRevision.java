package org.openl.rules.repository.api;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Identifies a branch tip and the content stored at one path in that tip.
 *
 * <p>A {@code null} tree revision means that the branch resolved successfully but the requested path is absent.
 * An unresolved branch is omitted from the result returned by
 * {@link BranchRepository#getBranchTreeRevisions(java.util.Collection, String)}.
 *
 * @param branchRevision the branch tip commit revision
 * @param treeRevision   the object revision at the requested path, or {@code null} when the path is absent
 * @param tipAffectsPath whether the branch tip changes the requested path relative to at least one parent
 */
public record BranchTreeRevision(@NonNull String branchRevision,
                                 @Nullable String treeRevision,
                                 boolean tipAffectsPath) {

    /**
     * Creates a conservative revision result for repositories that cannot distinguish a path-neutral tip.
     */
    public BranchTreeRevision(@NonNull String branchRevision, @Nullable String treeRevision) {
        this(branchRevision, treeRevision, true);
    }
}
