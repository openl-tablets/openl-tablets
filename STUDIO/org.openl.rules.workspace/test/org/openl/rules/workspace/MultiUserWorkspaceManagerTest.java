package org.openl.rules.workspace;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceManagerImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

class MultiUserWorkspaceManagerTest {
    @TempDir
    public File tempFolder;
    private MultiUserWorkspaceManager manager;
    private final WorkspaceUserImpl user = new WorkspaceUserImpl("user1",
            username -> new UserInfo("user1", "user1@email", "User1"));

    @BeforeEach
    void init() throws Exception {
        var localWorkspaceManager = new LocalWorkspaceManagerImpl();
        localWorkspaceManager.setWorkspaceHome(tempFolder.getAbsolutePath());
        localWorkspaceManager.init();

        manager = new MultiUserWorkspaceManager();
        manager.setLocalWorkspaceManager(localWorkspaceManager);
        manager.setDesignTimeRepository(new DesignTimeRepositoryImpl(null));
    }

    @Test
    void removeWorkspaceOnSessionTimeout() {
        var workspace1 = manager.getUserWorkspace(user);

        // Must return cached version
        var workspace2 = manager.getUserWorkspace(user);
        assertSame(workspace1, workspace2);

        // Session timeout
        workspace1.release();

        // Must create new instance
        workspace2 = manager.getUserWorkspace(user);
        assertNotSame(workspace1, workspace2);
    }

    @Test
    void theWorkspaceOutlivesEverySessionOfTheUserButTheLast() {
        var browser = manager.acquireUserWorkspace(user);
        var client = manager.acquireUserWorkspace(user);
        assertSame(browser, client);

        // One of the two ends: the other one goes on with the same workspace.
        manager.releaseUserWorkspace(browser);
        assertSame(client, manager.getUserWorkspaceIfCreated("user1"));

        // The last one ends: the workspace goes, and the next session starts a new one.
        manager.releaseUserWorkspace(client);
        assertNull(manager.getUserWorkspaceIfCreated("user1"));
        assertNotSame(client, manager.acquireUserWorkspace(user));
    }

    @Test
    void aWorkspaceThatFailsToBeReleasedHasNoHolderLeft() {
        var failing = mock(UserWorkspace.class);
        when(failing.getUser()).thenReturn(user);
        doThrow(new IllegalStateException("The disk is gone")).when(failing).release();
        manager.setUserWorkspaceFactory((local, design, owner) -> failing);

        manager.releaseUserWorkspace(manager.acquireUserWorkspace(user));

        // The count went with the failed release: the one holder that comes next releases the workspace again
        // when it ends, rather than being taken for the second of two.
        manager.releaseUserWorkspace(manager.acquireUserWorkspace(user));
        verify(failing, times(2)).release();
    }

}
