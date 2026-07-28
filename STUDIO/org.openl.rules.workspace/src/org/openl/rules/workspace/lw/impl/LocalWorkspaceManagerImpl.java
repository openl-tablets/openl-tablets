package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.util.FileUtils;

/**
 * LocalWorkspaceManager implementation.
 *
 * @author Aleh Bykhavets
 */
@Slf4j
public class LocalWorkspaceManagerImpl implements LocalWorkspaceManager, LocalWorkspaceListener {

    @Setter
    private String workspaceHome;
    @Setter
    private boolean enableLocks = true;

    // User name -> user workspace
    private final Map<String, LocalWorkspaceImpl> localWorkspaces = new HashMap<>();

    // User name -> metainfo registry. Registries are shared by all sessions of a user and are retained
    // for the JVM lifetime, so workspace instance churn cannot fork the local-changes state.
    private final Map<String, MetainfoRegistry> metainfoRegistries = new ConcurrentHashMap<>();

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
        var location = new File(workspaceHome);
        if (!location.mkdirs() && !location.exists()) {
            final String message = MessageFormat.format("Cannot create workspace location ''{0}''", workspaceHome);
            throw new FileNotFoundException(message);
        }
        log.info("Location of Local Workspaces: {}", workspaceHome);
    }

    private LocalWorkspaceImpl createWorkspace(String userId) {
        var userWorkspace = userDir(userId).toFile();
        log.debug("Workspace for user ''{}'' will be located at ''{}''", userId, userWorkspace.getAbsolutePath());
        var workspace = new LocalWorkspaceImpl(userId,
                userWorkspace,
                designTimeRepository,
                registryOf(userId));
        workspace.addWorkspaceListener(this);
        return workspace;
    }

    private MetainfoRegistry registryOf(String userId) {
        return metainfoRegistries.computeIfAbsent(userId, id -> MetainfoRegistry.open(userDir(id)));
    }

    /**
     * Resolves the workspace directory of the user.
     *
     * <p>The user id comes from the authentication and is used as a folder name, so it must stay
     * a single path component right under the workspace root. Otherwise the workspace operations,
     * including the registry reconciliation, could read or delete files outside the root.
     */
    private Path userDir(String userId) {
        var root = getWorkspaceHome();
        var userDir = root.resolve(userId).toAbsolutePath().normalize();
        if (!FolderHelper.isSafeFolderName(userId) || !root.equals(userDir.getParent())) {
            throw new IllegalArgumentException("The user id is not a valid workspace folder name.");
        }
        return userDir;
    }

    @Override
    public Path getWorkspaceHome() {
        return Path.of(workspaceHome).toAbsolutePath().normalize();
    }

    @Override
    public void refreshMetainfoRegistry(String userId) {
        var registry = metainfoRegistries.get(userId);
        if (registry == null) {
            // The first load performs the same reconciliation, so loading now is enough.
            registryOf(userId);
        } else {
            registry.refresh();
        }
    }

    @Override
    public LocalWorkspace getWorkspace(String userId) {
        var lwi = localWorkspaces.get(userId);
        if (lwi == null) {
            lwi = createWorkspace(userId);
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
            var lockEngine = lockEngines.get(type);
            if (lockEngine == null) {
                lockEngine = LockEngineImpl.create(new File(workspaceHome), type);
                lockEngines.put(type, lockEngine);
            }

            return lockEngine;
        }
    }

    @Override
    public void workspaceReleased(LocalWorkspace workspace) {
        workspace.removeWorkspaceListener(this);
        localWorkspaces.remove(((LocalWorkspaceImpl) workspace).getUserId());
    }
}
