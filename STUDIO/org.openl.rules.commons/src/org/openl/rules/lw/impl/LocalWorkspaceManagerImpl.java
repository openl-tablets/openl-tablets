package org.openl.rules.lw.impl;

import org.openl.rules.WorkspaceUser;
import org.openl.rules.WorkspaceException;
import org.openl.rules.commons.Utils;
import org.openl.rules.commons.util.SmartProps;
import org.openl.rules.lw.LocalWorkspace;
import org.openl.rules.lw.LocalWorkspaceManager;

import java.io.File;
import java.util.HashMap;

public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager {
    public static final String WS_PROPS = "workspace.properties";
    public static final String PROP_WS_LOCATION = "workspaces.location";
    public static final String DEF_WS_LOCATION = "/tmp/rules-workspaces/";

    /**
     * Root folder where all workspaces are.
     */
    private File workspacesLocation;

    /**
     * User name -> User Workspace
     */
    private HashMap<String, LocalWorkspaceImpl> localWorkspaces;

    public LocalWorkspaceManagerImpl() throws WorkspaceException {
        SmartProps props = new SmartProps(WS_PROPS);
        String wsLocation = props.getStr(PROP_WS_LOCATION, DEF_WS_LOCATION);

        workspacesLocation = new File(wsLocation);
        if (!Utils.checkOrCreateFolder(workspacesLocation)) {
            throw new WorkspaceException("Cannot create workspace location ''{0}''", wsLocation);
        }

        localWorkspaces = new HashMap<String, LocalWorkspaceImpl>();
    }

    public LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        LocalWorkspaceImpl lwi = localWorkspaces.get(userId);
        if (lwi == null) {
            lwi = createWorkspace(user);
            localWorkspaces.put(userId, lwi);
        }

        return lwi;
    }

// --- protected

    protected void notifyReleased(LocalWorkspaceImpl localWorkspace) {
        localWorkspaces.remove(localWorkspace.getUser().getUserId());
    }

    protected LocalWorkspaceImpl createWorkspace(WorkspaceUser user) throws WorkspaceException {
        String userId = user.getUserId();
        File f = Utils.generateSubLocation(workspacesLocation, userId);
        if (!Utils.checkOrCreateFolder(f)) {
            throw new WorkspaceException("Cannot create folder ''{0}'' for local workspace", f.getAbsolutePath());
        }

        return new LocalWorkspaceImpl(this, user, f);
    }
}
