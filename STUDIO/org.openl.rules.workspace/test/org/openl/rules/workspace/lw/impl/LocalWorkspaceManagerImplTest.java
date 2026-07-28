package org.openl.rules.workspace.lw.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.WorkspaceUserImpl;

class LocalWorkspaceManagerImplTest {
    @TempDir
    public File tempFolder;
    private LocalWorkspaceManagerImpl manager;

    @BeforeEach
    void init() throws Exception {
        manager = new LocalWorkspaceManagerImpl();
        manager.setWorkspaceHome(tempFolder.getAbsolutePath());
        manager.init();
    }

    @Test
    void removeWorkspaceOnSessionTimeout() {
        var user = new WorkspaceUserImpl("user.1",
                (username) -> new UserInfo("user.1", "user.1@email", "User 1"));
        var workspace1 = manager.getWorkspace(user.getUserId());
        var repoId = "design";

        // Must return cached version
        var workspace2 = manager.getWorkspace(user.getUserId());
        assertSame(workspace1, workspace2);

        // Session timeout
        workspace1.release();

        // Must create new instance
        workspace2 = manager.getWorkspace(user.getUserId());
        assertNotSame(workspace1, workspace2);
        assertNotSame(workspace1.getRepository(repoId), workspace2.getRepository(repoId));
    }

    @Test
    void dontCreateEmptyFolder() {
        var workspace1 = manager.getWorkspace(
                new WorkspaceUserImpl("user.1", (username) -> new UserInfo("user.1", "user.1@email", "User 1"))
                        .getUserId());
        assertFalse(workspace1.getLocation().exists());
    }

    @Test
    void rejectsUserIdEscapingTheWorkspaceRoot() {
        assertThrows(IllegalArgumentException.class, () -> manager.refreshMetainfoRegistry("../other"));
        assertThrows(IllegalArgumentException.class, () -> manager.refreshMetainfoRegistry("a/b"));
        assertThrows(IllegalArgumentException.class, () -> manager.refreshMetainfoRegistry(" "));
        assertThrows(IllegalArgumentException.class, () -> manager.refreshMetainfoRegistry(".hidden"));
        assertThrows(IllegalArgumentException.class, () -> manager.getWorkspace("../other"));
        assertFalse(new File(tempFolder.getParentFile(), "other").exists(),
                "Nothing must be created outside the workspace root.");
    }

    @Test
    void refreshMetainfoRegistryReconcilesTheUserWorkspace() throws IOException {
        var userDir = tempFolder.toPath().resolve("user.1");
        Files.createDirectories(userDir.resolve("stray"));

        // The registry is not loaded yet, so the load performs the reconciliation.
        manager.refreshMetainfoRegistry("user.1");
        assertFalse(Files.exists(userDir.resolve("stray")), "A folder without a record is garbage.");

        // The registry is live now and is shared with the workspace, so the refresh reconciles it.
        var workspace = manager.getWorkspace("user.1");
        Files.createDirectories(userDir.resolve("stray"));
        manager.refreshMetainfoRegistry("user.1");
        assertFalse(Files.exists(userDir.resolve("stray")));
        assertSame(workspace.getMetainfoRegistry(), manager.getWorkspace("user.1").getMetainfoRegistry());
    }
}
