package org.openl.studio.projects.messaging;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.studio.projects.service.ProjectStateChangedEvent;

/**
 * Watches the user workspaces on disk and reports every file change as a project change of its user.
 *
 * The disk is where every write ends up — the REST API, the legacy Editor, a user saving a workspace
 * file straight from Excel — so watching it catches them all at their one meeting point, the same way
 * the legacy Editor detects changes by the file timestamps. A change of
 * {@code <workspaces>/<user>/<project>/<file>} becomes a {@link ProjectStateChangedEvent} of that
 * user, carrying the project-relative file, and rides the ordinary ping pipeline from there.
 *
 * The locks tree ({@code <workspaces>/.locks}) is watched too, differently: a lock appearing or
 * releasing there changes the lock badge every user sees, so it broadcasts the projects-changed ping
 * instead of pinging one user. Other service entries (the registries — anything dot-prefixed) are not
 * project content and are ignored.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceFilesWatcher {

    private final MultiUserWorkspaceManager workspaceManager;
    private final ApplicationEventPublisher eventPublisher;
    private final LocalWorkspaceManager localWorkspaceManager;
    private final ProjectsChangedBroadcaster broadcaster;

    /** How long the watcher waits before retrying when the workspaces root cannot be walked yet. */
    private static final long REGISTER_RETRY_DELAY_MS = 10_000;

    private final Map<WatchKey, Path> watchedDirectories = new ConcurrentHashMap<>();
    private WatchService watchService;
    private Path root;

    @PostConstruct
    void start() throws IOException {
        // The manager created the folder while initializing — it comes in as a ready dependency.
        root = localWorkspaceManager.getWorkspaceHome();
        watchService = FileSystems.getDefault().newWatchService();
        var watcherThread = new Thread(this::watch, "openl-workspace-files-watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    @PreDestroy
    void stop() throws IOException {
        if (watchService != null) {
            watchService.close();
        }
    }

    private void watch() {
        // The watcher acts for the system: its thread has no session, and in multi-user mode the
        // ACL checks behind the workspace lookups need an authentication to answer — without one
        // they throw and every change would be swallowed. The thread is dedicated, so the context
        // is set once and never restored.
        SecurityContextHolder.getContext().setAuthentication(systemAuthentication());
        try {
            // The initial walk of all workspaces runs here, off the application startup path. A
            // transient failure (an unavailable mount, a permission race) must not silence the
            // watcher for the process's lifetime — it retries until the root becomes walkable.
            while (!tryRegisterRoot()) {
                Thread.sleep(REGISTER_RETRY_DELAY_MS);
            }
            while (true) {
                var key = watchService.take();
                var directory = watchedDirectories.get(key);
                for (var event : key.pollEvents()) {
                    if (directory != null && event.context() instanceof Path relative) {
                        onChanged(directory.resolve(relative), event.kind() == StandardWatchEventKinds.ENTRY_CREATE);
                    }
                }
                if (!key.reset()) {
                    watchedDirectories.remove(key);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException e) {
            // Shutdown: the service was closed — nothing more to watch.
        } catch (RuntimeException e) {
            log.warn("The workspace files watcher stopped unexpectedly.", e);
        }
    }

    /**
     * The system identity the watcher works under — the same one {@code ProjectVersionCacheMonitor}
     * uses for its background reads. The ACL passes for the system, and the ping it enables carries
     * no data: actual reads stay behind each user's own ACL.
     */
    private static Authentication systemAuthentication() {
        var group = new SimpleGroup();
        group.setName("ADMIN");
        var principal = SimpleUser.builder().setUsername("admin").setPrivileges(List.of(group)).build();
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    /** Package-private: the tests drive the retry decision directly. */
    boolean tryRegisterRoot() {
        try {
            registerTree(root);
            return true;
        } catch (IOException e) {
            log.warn("Failed to start watching the workspaces root '{}'. Will retry.", root, e);
            return false;
        }
    }

    /** Package-private: the tests feed paths straight in, past the platform-dependent watch timing. */
    void onChanged(Path changed, boolean created) {
        try {
            if (created && Files.isDirectory(changed)) {
                // A folder appeared (a project checkout, a new folder): watch what grows inside it too.
                registerTree(changed);
            }
            var relative = root.relativize(changed);
            if (isLockPath(relative)) {
                // A lock badge shows on every user's screens, so its change broadcasts like a commit.
                broadcaster.broadcastChanged();
                return;
            }
            if (relative.getNameCount() < 2 || isServicePath(relative)) {
                return;
            }
            var userFolder = relative.getName(0).toString();
            var projectName = relative.getName(1).toString();
            var paths = relative.getNameCount() > 2
                    ? List.of(relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/'))
                    : List.<String>of();
            publishChanged(userFolder, projectName, paths);
        } catch (RuntimeException | IOException e) {
            log.debug("Failed to handle a workspace file change of '{}'.", changed, e);
        }
    }

    /** Registries and any other dot-prefixed entries are bookkeeping, not project content. */
    private static boolean isServicePath(Path relative) {
        for (var segment : relative) {
            if (segment.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    /** The locks tree holds the project lock files whose changes move the lock badges of every user. */
    private static boolean isLockPath(Path relative) {
        return relative.getNameCount() > 0
                && LockEngineImpl.LOCKS_FOLDER_NAME.equals(relative.getName(0).toString());
    }

    /**
     * Reports the change as the ordinary project state event, so the existing pipeline debounces it
     * and pings the user's sessions. Advisory: a change that cannot be resolved to a workspace project
     * (e.g. it was just deleted, or the entry is not a project) is simply dropped — a deletion comes
     * with its own event.
     *
     * Peeks at the workspace cache instead of creating a workspace: the watcher does not know the
     * user's full identity, and a workspace created from a bare user would be cached and then sign
     * the real session's commits. No cached workspace also means no session to ping.
     */
    private void publishChanged(String userFolder, String projectName, List<String> paths) {
        try {
            var workspace = workspaceManager.getUserWorkspaceIfCreated(userFolder);
            if (workspace == null) {
                return;
            }
            var userName = WorkspaceUserImpl.decodeUserId(userFolder);
            // Looked up by name without a refresh: this runs on every file event of a busy write.
            for (var project : workspace.getProjectsByName(projectName, false)) {
                eventPublisher.publishEvent(new ProjectStateChangedEvent(project, userName, paths));
            }
        } catch (RuntimeException e) {
            log.debug("Skipped a change of workspace project '{}' in folder '{}'.", projectName, userFolder, e);
        }
    }

    private void registerTree(Path directory) throws IOException {
        try (Stream<Path> tree = Files.walk(directory)) {
            for (var current : tree.filter(Files::isDirectory).toList()) {
                var relative = root.relativize(current);
                if (isServicePath(relative) && !isLockPath(relative)) {
                    continue;
                }
                var key = current.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                watchedDirectories.put(key, current);
            }
        }
    }
}
