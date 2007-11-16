package org.openl.rules.workspace.lw.impl;

import org.openl.SmartProps;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;

import java.io.File;
import java.util.HashMap;

public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener {
    public static final String WS_PROPS = "workspace.properties";
    public static final String PROP_WS_LOCATION = "workspaces.location";
    public static final String DEF_WS_LOCATION = "/tmp/rules-workspaces/";

    /**
     * Root folder where all workspaces are.
     */
    private File workspacesLocation;

    /**
     * User name -> User Workspace.
     */
    private HashMap<String, LocalWorkspaceImpl> localWorkspaces;


    public LocalWorkspaceManagerImpl(SmartProps props) throws WorkspaceException {
        String wsLocation = props.getStr(PROP_WS_LOCATION, DEF_WS_LOCATION);

        workspacesLocation = new File(wsLocation);
        if (!FolderHelper.checkOrCreateFolder(workspacesLocation)) {
            throw new WorkspaceException("Cannot create workspace location ''{0}''", null, wsLocation);
        }

        localWorkspaces = new HashMap<String, LocalWorkspaceImpl>();
    }

    public LocalWorkspaceManagerImpl() throws WorkspaceException {
        this(new SmartProps(WS_PROPS));
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

        return new LocalWorkspaceImpl(user, f);
    }
}
