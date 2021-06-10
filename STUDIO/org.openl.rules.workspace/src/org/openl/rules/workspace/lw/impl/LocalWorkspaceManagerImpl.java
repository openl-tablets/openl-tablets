package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

/**
 * LocalWorkspaceManager implementation.
 *
 * @author Aleh Bykhavets
 */
public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener {
    private final Logger log = LoggerFactory.getLogger(LocalWorkspaceManagerImpl.class);

    private String workspaceHome;
    private boolean enableLocks = true;

    // User name -> user workspace
    private final Map<String, LocalWorkspaceImpl> localWorkspaces = new HashMap<>();

    // Project type (rules/deployment) -> Lock Engine
    private final Map<String, LockEngine> lockEngines = new HashMap<>();
    private final DesignTimeRepository designTimeRepository;

    // for tests
    public LocalWorkspaceManagerImpl() {
        designTimeRepository = null;
    }

    public LocalWorkspaceManagerImpl(PropertyResolver propertyResolver, DesignTimeRepository designTimeRepository) {
        workspaceHome = propertyResolver.getProperty("user.workspace.home");
        this.designTimeRepository = designTimeRepository;
    }

    /**
     * init-method
     */
    public void init() throws FileNotFoundException {
        if (workspaceHome == null) {
            log.warn("workspaceHome is not initialized. Default value is used.");
            workspaceHome = FileUtils.getTempDirectoryPath() + "/rules-workspaces/";
        }
        File location = new File(workspaceHome);
        if (!location.mkdirs() && !location.exists()) {
            final String message = MessageFormat.format("Cannot create workspace location ''{0}''", workspaceHome);
            throw new FileNotFoundException(message);
        }
        log.info("Location of Local Workspaces: {}", workspaceHome);
    }

    private LocalWorkspaceImpl createWorkspace(WorkspaceUser user) {
        String userId = user.getUserId();
        File workspaceRoot = new File(workspaceHome);
        File userWorkspace = new File(workspaceRoot, userId);
        log.debug("Workspace for user ''{}'' will be located at ''{}''", user.getUserId(), userWorkspace.getAbsolutePath());
        LocalWorkspaceImpl workspace = new LocalWorkspaceImpl(user, userWorkspace, designTimeRepository);
        workspace.addWorkspaceListener(this);
        return workspace;
    }

    @Override
    public LocalWorkspace getWorkspace(WorkspaceUser user) {
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
