package org.openl.rules.workspace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class MultiUserWorkspaceManager implements UserWorkspaceListener {
    /**
     * Design Time Repository
     */
    @Setter
    private DesignTimeRepository designTimeRepository;
    /**
     * Manager of Local Workspaces
     */
    @Setter
    private LocalWorkspaceManager localWorkspaceManager;
    /**
     * Cache for User Workspaces. Concurrent: request threads and the workspace files watcher's
     * background thread reach it at the same time.
     */
    private final Map<String, UserWorkspace> userWorkspaces = new ConcurrentHashMap<>();
    /**
     * How many holders the workspace of each user has: browser sessions and clients that call with their own
     * credentials.
     */
    private final Map<String, Integer> holders = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private UserWorkspaceFactory userWorkspaceFactory = new DefaultUserWorkspaceFactory();

    private UserWorkspace createUserWorkspace(WorkspaceUser user) {
        var userWorkspace = getUserWorkspaceFactory()
                .create(localWorkspaceManager, designTimeRepository, user);
        userWorkspace.addWorkspaceListener(this);
        return userWorkspace;
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
        var existing = userWorkspaces.get(user.getUserId());
        if (existing != null) {
            return existing;
        }
        // The creation (filesystem reads, a listener registration) runs under the map's lock on
        // purpose: two concurrent first requests of one user must not build two workspaces. It
        // happens once per user; the fast path above never takes the lock.
        return userWorkspaces.computeIfAbsent(user.getUserId(), id -> createUserWorkspace(user));
    }

    /**
     * Returns the workspace of the user and counts the caller among its holders.
     *
     * <p>Every holder hands the workspace back with {@link #releaseUserWorkspace(UserWorkspace)}. It is released
     * only when the last holder does, so one session of a user that ends leaves it working for the others.
     *
     * @param user active user
     * @return new or cached instance of user workspace
     */
    public UserWorkspace acquireUserWorkspace(WorkspaceUser user) {
        // Found and counted under the user's key, so a release of the last holder never runs between a new
        // holder finding the workspace and being counted. Once counted, the workspace stays for the caller.
        holders.compute(user.getUserId(), (id, count) -> {
            getUserWorkspace(user);
            return count == null ? 1 : count + 1;
        });
        return getUserWorkspace(user);
    }

    /**
     * Hands back a workspace taken with {@link #acquireUserWorkspace(WorkspaceUser)}, releasing it when no other
     * holder is left.
     *
     * @param workspace the workspace the caller holds
     */
    public void releaseUserWorkspace(UserWorkspace workspace) {
        holders.compute(workspace.getUser().getUserId(), (id, count) -> {
            if (count != null && count > 1) {
                return count - 1;
            }
            // The last holder is gone whatever the release comes to: a count left behind by a failed release
            // would keep the workspace from ever being released again.
            try {
                workspace.release();
            } catch (RuntimeException e) {
                log.error("Failed to release the workspace of the user '{}'.", id, e);
            }
            return null;
        });
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
