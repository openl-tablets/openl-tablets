package org.openl.studio.projects.service;

import java.util.List;

import org.openl.rules.project.abstraction.RulesProject;

/**
 * A project of the user's workspace changed: it was opened, closed, saved, deleted, its branch
 * changed, or its content changed on the workspace disk — reported by the files watcher whatever
 * wrote it: the REST API, the legacy Editor, a file saved straight from Excel.
 *
 * Published after the change happened, and consumed by the WebSocket layer to tell the user's other
 * sessions that what they show may be stale. Compile-status transitions are not published here: a
 * compile changes nothing this event stands for, and it already travels as
 * {@link org.openl.rules.ui.ProjectStatusChangedEvent} on the status destinations.
 *
 * @param project  the project after the action
 * @param userName the user whose workspace changed — the destination of the notification
 * @param paths    the project-relative files the change touched — a folder means anything under it;
 *                 empty when the change is project-wide or the files are unknown
 */
public record ProjectStateChangedEvent(RulesProject project, String userName, List<String> paths) {

    public ProjectStateChangedEvent(RulesProject project, String userName) {
        this(project, userName, List.of());
    }
}
