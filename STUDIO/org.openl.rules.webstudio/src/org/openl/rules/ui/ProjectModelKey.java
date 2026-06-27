package org.openl.rules.ui;

import jakarta.annotation.Nullable;

/**
 * Identity of a {@link ProjectModel} within a session: the project (repository + local folder) and the branch
 * it was opened on. A session keeps one model per key, so several projects compile independently and editing
 * one project never clobbers another's compiled state.
 *
 * <p>The same project opened on a different branch is a different key, so switching a project's branch never
 * reuses the previous branch's model.
 *
 * @param repositoryId      design repository id the project belongs to
 * @param projectFolderName local workspace folder name of the project (stable across both the JSF and REST
 *                          open paths, which resolve the same {@code ProjectDescriptor})
 * @param branch            branch the project is opened on; {@code null} for repositories without branches
 */
record ProjectModelKey(String repositoryId, String projectFolderName, @Nullable String branch) {

    /**
     * Sentinel key used when the session has no project selected. Its model stays empty (no module), so
     * {@code getModel()} never returns {@code null}.
     */
    static final ProjectModelKey NONE = new ProjectModelKey("", "", null);
}
