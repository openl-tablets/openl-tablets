package org.openl.rules.workspace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;

/**
 * Manager of Multiple User Workspaces.
 * <p/>
 * It takes care of creation and releasing of User Workspaces.
 * <p>
 * Must be configured in spring configuration as a singleton.
 *
 * @author Aleh Bykhavets
 */
public class MultiUserWorkspaceManager implements UserWorkspaceListener {
    /**
     * Design Time Repository
     */
    private DesignTimeRepository designTimeRepository;
    /**
     * Manager of Local Workspaces
     */
    private LocalWorkspaceManager localWorkspaceManager;
    /**
     * Cache for User Workspaces. Concurrent: request threads and the workspace files watcher's
     * background thread reach it at the same time.
     */
    private final Map<String, UserWorkspace> userWorkspaces = new ConcurrentHashMap<>();

    private UserWorkspaceFactory userWorkspaceFactory = new DefaultUserWorkspaceFactory();

    private UserWorkspace createUserWorkspace(WorkspaceUser user) {
        UserWorkspace userWorkspace = getUserWorkspaceFactory()
                .create(localWorkspaceManager, designTimeRepository, user);
        userWorkspace.addWorkspaceListener(this);
        return userWorkspace;
    }

    public UserWorkspaceFactory getUserWorkspaceFactory() {
        return userWorkspaceFactory;
    }

    public void setUserWorkspaceFactory(UserWorkspaceFactory userWorkspaceFactory) {
        this.userWorkspaceFactory = userWorkspaceFactory;
    }

    /**
     * Returns .
     * <p/>
     * It creates Workspace (including local) for specified user on first request.
     *
     * @param user active user
     * @return new or cached instance of user workspace
     */
    public UserWorkspace getUserWorkspace(WorkspaceUser user) {
        UserWorkspace existing = userWorkspaces.get(user.getUserId());
        if (existing != null) {
            return existing;
        }
        // The creation (filesystem reads, a listener registration) runs under the map's lock on
        // purpose: two concurrent first requests of one user must not build two workspaces. It
        // happens once per user; the fast path above never takes the lock.
        return userWorkspaces.computeIfAbsent(user.getUserId(), id -> createUserWorkspace(user));
    }

    /**
     * Returns the cached workspace of the user, or {@code null} when none exists yet.
     *
     * <p>Never creates one: a background caller does not know the user's full identity, and a
     * workspace created from a bare user would be cached and then sign the real session's commits.
     *
     * @param userId the user id, as {@link WorkspaceUser#getUserId()} returns it
     */
    public UserWorkspace getUserWorkspaceIfCreated(String userId) {
        return userWorkspaces.get(userId);
    }

    public void setDesignTimeRepository(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void setLocalWorkspaceManager(LocalWorkspaceManager localWorkspaceManager) {
        this.localWorkspaceManager = localWorkspaceManager;
    }

    /**
     * UserWorkspace should notify manager that life cycle of the workspace is ended and it must be removed from cache.
     */
    @Override
    public void workspaceReleased(UserWorkspace workspace) {
        workspace.removeWorkspaceListener(this);
        userWorkspaces.remove(workspace.getUser().getUserId());
    }

    public void refreshWorkspaces() {
        userWorkspaces.values().forEach(UserWorkspace::refresh);
    }
}
