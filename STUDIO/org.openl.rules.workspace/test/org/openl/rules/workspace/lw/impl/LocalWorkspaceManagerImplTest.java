package org.openl.rules.workspace.lw.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.lw.LocalWorkspace;

public class LocalWorkspaceManagerImplTest {
    @TempDir
    public File tempFolder;
    private LocalWorkspaceManagerImpl manager;

    @BeforeEach
    public void init() throws Exception {
        manager = new LocalWorkspaceManagerImpl();
        manager.setWorkspaceHome(tempFolder.getAbsolutePath());
        manager.init();
    }

    @Test
    public void removeWorkspaceOnSessionTimeout() {
        WorkspaceUserImpl user = new WorkspaceUserImpl("user.1",
                (username) -> new UserInfo("user.1", "user.1@email", "User 1"));
        LocalWorkspace workspace1 = manager.getWorkspace(user.getUserId());
        String repoId = "design";

        // Must return cached version
        LocalWorkspace workspace2 = manager.getWorkspace(user.getUserId());
        assertSame(workspace1, workspace2);

        // Session timeout
        workspace1.release();

        // Must create new instance
        workspace2 = manager.getWorkspace(user.getUserId());
        assertNotSame(workspace1, workspace2);
        assertNotSame(workspace1.getRepository(repoId), workspace2.getRepository(repoId));
    }

    @Test
    public void dontCreateEmptyFolder() {
        LocalWorkspace workspace1 = manager.getWorkspace(
                new WorkspaceUserImpl("user.1", (username) -> new UserInfo("user.1", "user.1@email", "User 1"))
                        .getUserId());
        assertFalse(workspace1.getLocation().exists());
    }
}