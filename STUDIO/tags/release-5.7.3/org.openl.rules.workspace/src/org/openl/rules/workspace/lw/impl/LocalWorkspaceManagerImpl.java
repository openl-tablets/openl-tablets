package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.security.SecurityUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.util.MsgHelper;
import org.springframework.beans.factory.InitializingBean;

/**
 * LocalWorkspaceManager implementation.
 *
 * @author Aleh Bykhavets
 */
public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener, InitializingBean {
    private static final Log log = LogFactory.getLog(LocalWorkspaceManagerImpl.class);

    private String workspacesRoot = "/tmp/rules-workspaces/";
    private boolean autoLogin = false;
    private FileFilter localWorkspaceFolderFilter;
    private FileFilter localWorkspaceFileFilter;

    // User name -> user workspace
    private Map<String, LocalWorkspaceImpl> localWorkspaces = new HashMap<String, LocalWorkspaceImpl>();

    public void afterPropertiesSet() throws Exception {
        if (!FolderHelper.checkOrCreateFolder(new File(workspacesRoot))) {
            throw new WorkspaceException("Cannot create workspace location ''{0}''", null, workspacesRoot);
        }
        log.info("Location of Local Workspaces: " + workspacesRoot);
        log.info("Allow local user:" + autoLogin);
    }

    protected LocalWorkspaceImpl createEclipseWorkspace(WorkspaceUser user) throws WorkspaceException {
        String eclipseWorkspacePath = System.getProperty("openl.webstudio.home");
        if (eclipseWorkspacePath == null) {
            eclipseWorkspacePath = "..";
        }
        log.debug(MsgHelper.format("Referencing eclipse workspace for user ''{0}'' at ''{1}''", user.getUserId(),
                eclipseWorkspacePath));
        return new LocalWorkspaceImpl(user, new File(eclipseWorkspacePath), localWorkspaceFolderFilter,
                localWorkspaceFileFilter);
    }

    protected LocalWorkspaceImpl createWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        File f = FolderHelper.generateSubLocation(new File(workspacesRoot), userId);
        if (!FolderHelper.checkOrCreateFolder(f)) {
            throw new WorkspaceException("Cannot create folder ''{0}'' for local workspace!", null, f.getAbsolutePath());
        }
        log.debug(MsgHelper.format("Creating workspace for user ''{0}'' at ''{1}''", user.getUserId(), f
                .getAbsolutePath()));
        return new LocalWorkspaceImpl(user, f, localWorkspaceFolderFilter, localWorkspaceFileFilter);
    }

    public LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        LocalWorkspaceImpl lwi = localWorkspaces.get(userId);
        if (lwi == null) {
            if (autoLogin && SecurityUtils.LOCAL_USER_ID.equals(userId)) {
                lwi = createEclipseWorkspace(user);
            } else {
                lwi = createWorkspace(user);
            }
            localWorkspaces.put(userId, lwi);
        }
        return lwi;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }

    public void setLocalWorkspaceFolderFilter(FileFilter localWorkspaceFolderFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
    }

    public void setWorkspacesRoot(String workspacesRoot) {
        this.workspacesRoot = workspacesRoot;
    }

    public void workspaceReleased(LocalWorkspace workspace) {
        localWorkspaces.remove(((LocalWorkspaceImpl) workspace).getUser().getUserId());
    }
}
