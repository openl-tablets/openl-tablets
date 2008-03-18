package org.openl.rules.workspace.lw.impl;

import org.openl.SmartProps;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;

public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener {
    public static final String WS_PROPS = "workspace.properties";

    public static final String PROP_WS_LOCATION = "workspaces.location";
    public static final String DEF_WS_LOCATION = "/tmp/rules-workspaces/";

    public static final String PROP_WS_ECLIPSE_4_LOCAL_USER = "workspaces.useEclipseForLocalUser";
    public static final String DEF_WS_ECLIPSE_4_LOCAL_USER = Boolean.FALSE.toString();

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

    public LocalWorkspaceManagerImpl(SmartProps props) throws WorkspaceException {
        String wsLocation = props.getStr(PROP_WS_LOCATION, DEF_WS_LOCATION);
        String s = props.getStr(PROP_WS_ECLIPSE_4_LOCAL_USER, DEF_WS_ECLIPSE_4_LOCAL_USER);
        useEclipse4LocalUser = Boolean.parseBoolean(s);

        workspacesLocation = new File(wsLocation);
        if (!FolderHelper.checkOrCreateFolder(workspacesLocation)) {
            throw new WorkspaceException("Cannot create workspace location ''{0}''", null, wsLocation);
        }

        Log.debug("Location of Local Workspaces: ''{0}''", wsLocation);
        Log.debug("Use eclipse for local user ''{1}'': ''{0}''", useEclipse4LocalUser, USER_LOCAL);

        localWorkspaces = new HashMap<String, LocalWorkspaceImpl>();
    }

    public LocalWorkspaceManagerImpl() throws WorkspaceException {
        this(new SmartProps(WS_PROPS));
    }

    public LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        LocalWorkspaceImpl lwi = localWorkspaces.get(userId);
        if (lwi == null) {
            if (USER_LOCAL.equals(userId) && useEclipse4LocalUser) {
                lwi = createEclipseWorkspace(user);
            } else {
                lwi = createWorkspace(user);
            }
            localWorkspaces.put(userId, lwi);
        }

        return lwi;
    }

    public void workspaceReleased(LocalWorkspace workspace) {
        localWorkspaces.remove(((LocalWorkspaceImpl)workspace).getUser().getUserId());
    }

// --- protected

    protected LocalWorkspaceImpl createWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        File f = FolderHelper.generateSubLocation(workspacesLocation, userId);
        if (!FolderHelper.checkOrCreateFolder(f)) {
            throw new WorkspaceException("Cannot create folder ''{0}'' for local workspace", null, f.getAbsolutePath());
        }

        Log.debug("Creating workspace for user ''{0}'' at ''{1}''", user.getUserId(), f.getAbsolutePath());
        return new LocalWorkspaceImpl(user, f, localWorkspaceFolderFilter, localWorkspaceFileFilter);
    }

    protected LocalWorkspaceImpl createEclipseWorkspace(WorkspaceUser user) throws WorkspaceException {
        String eclipseWorkspacePath = System.getProperty("openl.webstudio.home");
        if (eclipseWorkspacePath == null) {
            eclipseWorkspacePath = "..";
        }

        Log.debug("Referencing eclipse workspace for user ''{0}'' at ''{1}''", user.getUserId(), eclipseWorkspacePath);
        return new LocalWorkspaceImpl(user, new File(eclipseWorkspacePath), localWorkspaceFolderFilter, localWorkspaceFileFilter);
    }

    public boolean isUseEclipse4LocalUser() {
        return useEclipse4LocalUser;
    }
}
