package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LocalWorkspaceManager implementation.
 *
 * @author Aleh Bykhavets
 */
public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener {
    private final Logger log = LoggerFactory.getLogger(LocalWorkspaceManagerImpl.class);

    private String workspaceHome;
    private FileFilter localWorkspaceFolderFilter;
    private FileFilter localWorkspaceFileFilter;
    private boolean enableLocks = true;

    // User name -> user workspace
    private Map<String, LocalWorkspaceImpl> localWorkspaces = new HashMap<>();

    // Project type (rules/deployment) -> Lock Engine
    private final Map<String, LockEngine> lockEngines = new HashMap<>();

    /**
     * init-method
     */
    public void init() throws Exception {
        if (workspaceHome == null) {
            log.warn("workspaceHome is not initialized. Default value is used.");
            workspaceHome = FileUtils.getTempDirectoryPath() + "/rules-workspaces/";
        }
        if (!FolderHelper.checkOrCreateFolder(new File(workspaceHome))) {
            throw new WorkspaceException("Cannot create workspace location ''{0}''", null, workspaceHome);
        }
        log.info("Location of Local Workspaces: {}", workspaceHome);
    }

    protected LocalWorkspaceImpl createWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        File workspaceRoot = new File(workspaceHome);
        File userWorkspace = new File(workspaceRoot, userId);
        if (!FolderHelper.checkOrCreateFolder(userWorkspace)) {
            throw new WorkspaceException("Cannot create folder ''{0}'' for local workspace.",
                null,
                userWorkspace.getAbsolutePath());
        }
        log.debug("Creating workspace for user ''{}'' at ''{}''", user.getUserId(), userWorkspace.getAbsolutePath());
        LocalWorkspaceImpl workspace = new LocalWorkspaceImpl(user,
            userWorkspace,
            localWorkspaceFolderFilter,
            localWorkspaceFileFilter);
        workspace.addWorkspaceListener(this);
        return workspace;
    }

    @Override
    public LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        LocalWorkspaceImpl lwi = localWorkspaces.get(userId);
        if (lwi == null) {
            lwi = createWorkspace(user);
            localWorkspaces.put(userId, lwi);
        }
        return lwi;
    }

    @Override
    public LockEngine getLockEngine(String type) {
        if (!enableLocks) {
            return new DummyLockEngine();
        }
        synchronized (lockEngines) {
            LockEngine lockEngine = lockEngines.get(type);
            if (lockEngine == null) {
                lockEngine = LockEngineImpl.create(new File(workspaceHome), type);
                lockEngines.put(type, lockEngine);
            }

            return lockEngine;
        }
    }

    public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }

    public void setLocalWorkspaceFolderFilter(FileFilter localWorkspaceFolderFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
    }

    public void setWorkspaceHome(String workspaceHome) {
        this.workspaceHome = workspaceHome;
    }

    public void setEnableLocks(boolean enableLocks) {
        this.enableLocks = enableLocks;
    }

    @Override
    public void workspaceReleased(LocalWorkspace workspace) {
        workspace.removeWorkspaceListener(this);
        localWorkspaces.remove(((LocalWorkspaceImpl) workspace).getUser().getUserId());
    }
}
