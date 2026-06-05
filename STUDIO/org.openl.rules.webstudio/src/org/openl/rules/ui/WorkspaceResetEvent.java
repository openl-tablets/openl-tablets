package org.openl.rules.ui;

import org.springframework.context.ApplicationEvent;

/**
 * Published by {@link WebStudio#reset()} when the session workspace is reset and any
 * per-session derived caches become stale. Session-scoped services that cache project
 * results — compile status, test, run and trace results — subscribe to this event and
 * drop their cached entries so the next request recomputes from the current model state.
 *
 * <p>The event carries no payload: it is a pure "invalidate your caches" signal. It is
 * delivered synchronously on the request thread, so session-scoped listeners resolve to
 * the session being reset.
 */
public class WorkspaceResetEvent extends ApplicationEvent {

    public WorkspaceResetEvent(Object source) {
        super(source);
    }
}
