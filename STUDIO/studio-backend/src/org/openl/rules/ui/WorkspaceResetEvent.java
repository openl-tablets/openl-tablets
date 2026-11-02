package org.openl.rules.ui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import org.openl.rules.workspace.ProjectId;

/**
 * Published by {@link WebStudio} when the session workspace is reset and any per-session derived caches become
 * stale. Session-scoped services that cache project results — compile status, test, run and trace results —
 * subscribe to this event and drop their cached entries so the next request recomputes from the current model state.
 *
 * <p>An edit of the module open concerns its project alone; a reset of the projects of the session concerns every
 * project. A cache that keeps the results of one project may keep them when another project is reset.
 *
 * <p>It is delivered synchronously on the request thread, so session-scoped listeners resolve to the session being
 * reset.
 */
public class WorkspaceResetEvent extends ApplicationEvent {

    private final @Nullable ProjectId projectId;

    /** A reset of every project of the session. */
    public WorkspaceResetEvent(@NonNull Object source) {
        this(source, null);
    }

    /**
     * @param projectId the project reset, or {@code null} for every project of the session
     */
    public WorkspaceResetEvent(@NonNull Object source, @Nullable ProjectId projectId) {
        super(source);
        this.projectId = projectId;
    }

    /** Whether what was worked out for the project is stale. */
    public boolean concerns(ProjectId project) {
        return projectId == null || projectId.equals(project);
    }
}
