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
import org.openl.rules.workspace.uw.UserWorkspace;

public class MultiUserWorkspaceManagerTest {
    @TempDir
    public File tempFolder;
    private MultiUserWorkspaceManager manager;

    @BeforeEach
    public void init() throws Exception {
        LocalWorkspaceManagerImpl localWorkspaceManager = new LocalWorkspaceManagerImpl();
        localWorkspaceManager.setWorkspaceHome(tempFolder.getAbsolutePath());
        localWorkspaceManager.init();

        manager = new MultiUserWorkspaceManager();
        manager.setLocalWorkspaceManager(localWorkspaceManager);
        manager.setDesignTimeRepository(new DesignTimeRepositoryImpl(null, null));
    }

    @Test
    public void removeWorkspaceOnSessionTimeout() {
        WorkspaceUserImpl user = new WorkspaceUserImpl("user1",
                (username) -> new UserInfo("user1", "user1@email", "User1"));
        UserWorkspace workspace1 = manager.getUserWorkspace(user);

        // Must return cached version
        UserWorkspace workspace2 = manager.getUserWorkspace(user);
        assertSame(workspace1, workspace2);

        // Session timeout
        workspace1.release();

        // Must create new instance
        workspace2 = manager.getUserWorkspace(user);
        assertNotSame(workspace1, workspace2);
    }

}
