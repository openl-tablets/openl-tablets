package org.openl.rules.workspace.lw;

import java.io.File;
import java.util.List;

import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.workspace.abstracts.ProjectsContainer;

/**
 * A container for <code>LocalProject</code>s. It is supposed to be able to store and restore projects on/from
 * filesystem.
 *
 * @author Aleh Bykhavets
 */
public interface LocalWorkspace extends ProjectsContainer {

    /**
     * Adds listener to the workspace that listens to workspace events.
     *
     * @param listener workspace listener.
     */
    void addWorkspaceListener(LocalWorkspaceListener listener);

    LocalRepository getRepository(String id);

    /**
     * Returns directory in the filesystem used storage for workspace projects.
     *
     * @return storage directory for workspace projects
     */
    File getLocation();

    /**
     * Refreshes the projects and their contents according to the changes in filesystem location that is used as storage
     * for workspace projects.
     */
    void refresh();

    /**
     * The method should be called when working with the workspace is finished. It saves projects' state and releases
     * resources.
     */
    void release();

    /**
     * Removes a listener from workspace. If there is no such listener nothing happens.
     *
     * @param listener listener to remove
     */
    void removeWorkspaceListener(LocalWorkspaceListener listener);

}
