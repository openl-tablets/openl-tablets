package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigPropertyBoolean;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.SysConfigManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.util.MsgHelper;

public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener {
    private static final Log log = LogFactory.getLog(LocalWorkspaceManagerImpl.class);
    public static final String WS_PROPS = "workspace.properties";
    public static final String PROP_WS_LOCATION = "workspaces.location";
    private final ConfigPropertyString confWSLocation = new ConfigPropertyString(PROP_WS_LOCATION,
            "/tmp/rules-workspaces/");
    private final ConfigPropertyBoolean confUseEclipse4Localuser = new ConfigPropertyBoolean(
            "workspaces.useEclipseForLocalUser", false);
    public static final String USER_LOCAL = "LOCAL";
    /**
     * Root folder where all workspaces are.
     */
    private File workspacesLocation;
    private boolean useEclipse4LocalUser;
    /**
     * User name -> User Workspace.
     */
    private HashMap<String, LocalWorkspaceImpl> localWorkspaces;
    private FileFilter localWorkspaceFolderFilter;
    private FileFilter localWorkspaceFileFilter;

    public void setLocalWorkspaceFolderFilter(FileFilter localWorkspaceFolderFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
    }

    public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }

    public LocalWorkspaceManagerImpl(ConfigSet confSet) throws WorkspaceException {
        if (confSet != null) {
            confSet.updateProperty(confWSLocation);
            confSet.updateProperty(confUseEclipse4Localuser);
        }
        String wsLocation = confWSLocation.getValue();
        useEclipse4LocalUser = confUseEclipse4Localuser.getValue();
        workspacesLocation = new File(wsLocation);
        if (!FolderHelper.checkOrCreateFolder(workspacesLocation)) {
            throw new WorkspaceException("Cannot create workspace location ''{0}''", null, wsLocation);
        }
        log.info(MsgHelper.format("Location of Local Workspaces: ''{0}''", wsLocation));
        log.info(MsgHelper.format("Use eclipse for local user ''{1}'': ''{0}''", useEclipse4LocalUser, USER_LOCAL));
        localWorkspaces = new HashMap<String, LocalWorkspaceImpl>();
    }

    public LocalWorkspaceManagerImpl() throws WorkspaceException {
        this(SysConfigManager.getConfigManager().locate(WS_PROPS));
    }

    public LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        LocalWorkspaceImpl lwi = localWorkspaces.get(userId);
        if (lwi == null) {
            if (USER_LOCAL.equals(userId) && useEclipse4LocalUser) {
                lwi = createEclipseWorkspace(user);
            }
            else {
                lwi = createWorkspace(user);
            }
            localWorkspaces.put(userId, lwi);
        }
        return lwi;
    }

    public void workspaceReleased(LocalWorkspace workspace) {
        localWorkspaces.remove(((LocalWorkspaceImpl) workspace).getUser().getUserId());
    }

    // --- protected
    protected LocalWorkspaceImpl createWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        File f = FolderHelper.generateSubLocation(workspacesLocation, userId);
        if (!FolderHelper.checkOrCreateFolder(f)) {
            throw new WorkspaceException("Cannot create folder ''{0}'' for local workspace!", null, f.getAbsolutePath());
        }
        log.debug(MsgHelper.format("Creating workspace for user ''{0}'' at ''{1}''", user.getUserId(), f
                .getAbsolutePath()));
        return new LocalWorkspaceImpl(user, f, localWorkspaceFolderFilter, localWorkspaceFileFilter);
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

    public boolean isUseEclipse4LocalUser() {
        return useEclipse4LocalUser;
    }
}
