package org.openl.rules.workspace.lw;

import java.nio.file.Path;

import org.openl.rules.project.abstraction.LockEngine;

public interface LocalWorkspaceManager {
    LocalWorkspace getWorkspace(String userId);

    /**
     * The root folder on disk holding the workspaces of all users, one subfolder per user.
     */
    Path getWorkspaceHome();

    /**
     * @param type projects type, used as a subfolder name. For example "rules" or "deployments"
     */
    LockEngine getLockEngine(String type);

    /**
     * Reconciles the metainfo registry of the user workspace with the disk state and recomputes
     * the local-changes state of the workspace projects. Called on user sign-in.
     *
     * <p>When the registry of the user is not loaded yet, it is loaded now; the load performs the
     * same reconciliation.
     */
    void refreshMetainfoRegistry(String userId);
}
