package org.openl.studio.projects.service.trace;

import java.util.concurrent.atomic.AtomicReference;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.workspace.ProjectId;

/**
 * The executed tree of a finished business-view run, kept after its debug session ends.
 *
 * <p>The business view lists the upper levels of the tree first and loads a deep branch only when it is expanded.
 * A click on a row re-runs the calculation in a new debug session, which records no tree, so the branches are read
 * from this snapshot instead. Loading a branch never runs the calculation again.
 *
 * <p>The snapshot belongs to one run, named by the id of its debug session, which the registry keeps it under. Once
 * released, the snapshot holds no tree.
 */
public final class TraceTreeSnapshot {

    /** The id of the debug session that recorded the tree. */
    @Getter(AccessLevel.PACKAGE)
    private final String runId;

    private final ProjectId projectId;

    /** How many nodes the tree holds, so the kept trees of all users can be bounded by their size. */
    @Getter
    private final long nodeCount;

    private final AtomicReference<@Nullable CallNode> root;

    /** Wall-clock of the last access; drives idle release. */
    @Getter
    private volatile long lastAccessMillis = System.currentTimeMillis();

    public TraceTreeSnapshot(String runId, ProjectId projectId, CallNode root, long nodeCount) {
        this.runId = runId;
        this.projectId = projectId;
        this.nodeCount = nodeCount;
        this.root = new AtomicReference<>(root);
    }

    /**
     * The root of the kept tree, if it was recorded in the given project and is not released yet. A read counts as
     * an access, so a tree being browsed is not released as idle.
     */
    public @Nullable CallNode tree(ProjectId projectId) {
        var tree = root.get();
        if (tree == null || !this.projectId.equals(projectId)) {
            return null;
        }
        touch();
        return tree;
    }

    /** Record an access, so a tree whose window is in use is not released as idle. */
    void touch() {
        lastAccessMillis = System.currentTimeMillis();
    }

    /** Whether the tree was released. */
    boolean isReleased() {
        return root.get() == null;
    }

    /** Drop the tree, so its memory is freed even while something still refers to the snapshot. */
    public void release() {
        root.set(null);
    }
}
