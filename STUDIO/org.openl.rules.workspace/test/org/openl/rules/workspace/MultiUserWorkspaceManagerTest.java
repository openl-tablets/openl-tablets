package org.openl.rules.workspace;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceManagerImpl;

class MultiUserWorkspaceManagerTest {
    @TempDir
    public File tempFolder;
    private MultiUserWorkspaceManager manager;

    @BeforeEach
    void init() throws Exception {
        var localWorkspaceManager = new LocalWorkspaceManagerImpl();
        localWorkspaceManager.setWorkspaceHome(tempFolder.getAbsolutePath());
        localWorkspaceManager.init();

        manager = new MultiUserWorkspaceManager();
        manager.setLocalWorkspaceManager(localWorkspaceManager);
        manager.setDesignTimeRepository(new DesignTimeRepositoryImpl(null, null));
    }

    @Test
    void removeWorkspaceOnSessionTimeout() {
        var user = new WorkspaceUserImpl("user1",
                (username) -> new UserInfo("user1", "user1@email", "User1"));
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

}
