package org.openl.studio.projects.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.service.ProjectStateChangedEvent;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceFilesWatcherTest {

    @TempDir
    private Path workspacesRoot;

    @Mock
    private MultiUserWorkspaceManager workspaceManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LocalWorkspaceManager localWorkspaceManager;

    @Mock
    private ProjectsChangedBroadcaster broadcaster;

    @Mock
    private UserWorkspace userWorkspace;

    @Mock
    private RulesProject project;

    @InjectMocks
    private WorkspaceFilesWatcher watcher;

    @BeforeEach
    void setUp() throws Exception {
        when(localWorkspaceManager.getWorkspaceHome()).thenReturn(workspacesRoot);
        when(workspaceManager.getUserWorkspaceIfCreated(any())).thenReturn(userWorkspace);
        when(userWorkspace.getProjectsByName("Alpha", false)).thenReturn(List.of(project));
        watcher.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        watcher.stop();
    }

    @Test
    void a_file_change_becomes_a_state_change_of_its_user_and_project() throws Exception {
        var file = workspacesRoot.resolve("jane/Alpha/rules/Main.xlsx");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");

        watcher.onChanged(file, false);

        var captor = ArgumentCaptor.forClass(ProjectStateChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("jane", captor.getValue().userName());
        assertEquals(List.of("rules/Main.xlsx"), captor.getValue().paths());
    }

    @Test
    void the_workspace_folder_name_decodes_back_to_the_principal() throws Exception {
        // "a.b" is stored on disk as "a(2e)b" — the ping must reach the real user, not the folder name.
        var file = workspacesRoot.resolve("a(2e)b/Alpha/rules.xml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");

        watcher.onChanged(file, false);

        var captor = ArgumentCaptor.forClass(ProjectStateChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("a.b", captor.getValue().userName());
    }

    @Test
    void a_lock_change_broadcasts_to_everyone_instead_of_pinging_one_user() throws Exception {
        // Another user locking or releasing a project moves the lock badge on every user's screens.
        var lock = workspacesRoot.resolve(".locks/rules/design/Alpha/ready.lock");
        Files.createDirectories(lock.getParent());
        Files.writeString(lock, "user=john");

        watcher.onChanged(lock, false);

        verify(broadcaster).broadcastChanged();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void service_entries_are_bookkeeping_and_never_ping() throws Exception {
        var registry = workspacesRoot.resolve("jane/.registries/projects.json");
        Files.createDirectories(registry.getParent());
        Files.writeString(registry, "x");

        watcher.onChanged(registry, false);

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(broadcaster);
    }

    @Test
    void a_change_that_resolves_to_no_workspace_project_is_dropped_quietly() throws Exception {
        when(userWorkspace.getProjectsByName("Gone", false)).thenThrow(new IllegalStateException("no such project"));
        var file = workspacesRoot.resolve("jane/Gone/file.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");

        watcher.onChanged(file, false);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void a_user_without_a_created_workspace_gets_no_ping_and_no_workspace_is_created() throws Exception {
        // Nobody is logged in as this user: creating a workspace here would cache one built from a
        // bare user, and the real session's commits would then carry an empty identity.
        when(workspaceManager.getUserWorkspaceIfCreated(any())).thenReturn(null);
        var file = workspacesRoot.resolve("ghost/Alpha/rules.xml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");

        watcher.onChanged(file, false);

        verify(workspaceManager, never()).getUserWorkspace(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void a_user_level_entry_is_not_project_content() throws Exception {
        var registry = workspacesRoot.resolve("jane");
        Files.createDirectories(registry);

        watcher.onChanged(registry, false);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void keeps_retrying_until_the_workspaces_root_becomes_walkable() throws Exception {
        // The root cannot be walked yet (an unavailable mount at startup): the watcher must retry
        // instead of dying for the process's lifetime.
        var lateRoot = workspacesRoot.resolve("late").resolve("workspaces");
        when(localWorkspaceManager.getWorkspaceHome()).thenReturn(lateRoot);
        var lateWatcher = new WorkspaceFilesWatcher(workspaceManager, eventPublisher, localWorkspaceManager, broadcaster);
        lateWatcher.start();
        try {
            assertFalse(lateWatcher.tryRegisterRoot());

            Files.createDirectories(lateRoot);
            assertTrue(lateWatcher.tryRegisterRoot());
        } finally {
            lateWatcher.stop();
        }
    }
}
