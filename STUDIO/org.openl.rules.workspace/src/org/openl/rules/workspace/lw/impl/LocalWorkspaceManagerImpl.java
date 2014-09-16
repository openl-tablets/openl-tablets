package org.openl.rules.workspace.lw.impl;

import static org.apache.commons.io.FileUtils.getTempDirectoryPath;

import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

/**
 * LocalWorkspaceManager implementation.
 *
 * @author Aleh Bykhavets
 */
public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener, InitializingBean {
    private final Logger log = LoggerFactory.getLogger(LocalWorkspaceManagerImpl.class);

    private String workspaceHome;
    private boolean singleUserMode = false;
    private FileFilter localWorkspaceFolderFilter;
    private FileFilter localWorkspaceFileFilter;

    // User name -> user workspace
    private Map<String, LocalWorkspaceImpl> localWorkspaces = new HashMap<String, LocalWorkspaceImpl>();

    public void afterPropertiesSet() throws Exception {
        if (workspaceHome == null) {
            log.warn("workspaceHome isn't initialized. Default value is used.");
            workspaceHome = getTempDirectoryPath() + "/rules-workspaces/";
        }
        if (!FolderHelper.checkOrCreateFolder(new File(workspaceHome))) {
            throw new WorkspaceException("Cannot create workspace location ''{0}''", null, workspaceHome);
        }
        log.info("Location of Local Workspaces: {}\nSingle user mode: {}", workspaceHome, singleUserMode);
    }

    protected LocalWorkspaceImpl createSingleUserWorkspace(WorkspaceUser user) throws WorkspaceException {
        log.debug("Workspace home: ''{}''", workspaceHome);
        File localWorkspaceDir = new File(workspaceHome);
        if (!localWorkspaceDir.exists()) {
            localWorkspaceDir.mkdir();
        }
        return new LocalWorkspaceImpl(user, localWorkspaceDir, localWorkspaceFolderFilter,
                localWorkspaceFileFilter);
    }

    protected LocalWorkspaceImpl createWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        File f = FolderHelper.generateSubLocation(new File(workspaceHome), userId);
        if (!FolderHelper.checkOrCreateFolder(f)) {
            throw new WorkspaceException("Cannot create folder ''{0}'' for local workspace!", null, f.getAbsolutePath());
        }
        log.debug("Creating workspace for user ''{}'' at ''{}''", user.getUserId(), f.getAbsolutePath());
        return new LocalWorkspaceImpl(user, f, localWorkspaceFolderFilter, localWorkspaceFileFilter);
    }

    public LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        LocalWorkspaceImpl lwi = localWorkspaces.get(userId);
        if (lwi == null) {
            if (singleUserMode) {
                lwi = createSingleUserWorkspace(user);
            } else {
                lwi = createWorkspace(user);
            }
            localWorkspaces.put(userId, lwi);
        }
        return lwi;
    }

    public void setSingleUserMode(boolean singleUserMode) {
        this.singleUserMode = singleUserMode;
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

    public void workspaceReleased(LocalWorkspace workspace) {
        localWorkspaces.remove(((LocalWorkspaceImpl) workspace).getUser().getUserId());
    }
}
