package org.openl.rules.project.impl.local;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Releasing the locks of a repository frees every project of it, in every branch, and keeps the locks of the other
 * repositories.
 *
 * @author Yury Molchan
 */
class LockEngineImplTest {

    @TempDir
    Path workspaces;

    @Test
    void releasesTheLocksOfOneRepositoryOnly() {
        var engine = LockEngineImpl.create(workspaces.toFile(), "projects");
        assertTrue(engine.tryLock("design", null, "P1", "user1"));
        assertTrue(engine.tryLock("design", "feature/x", "P2", "user1"));
        assertTrue(engine.tryLock("design2", null, "P1", "user1"));

        engine.forceUnlockRepository("design");

        assertFalse(engine.getLockInfo("design", null, "P1").isLocked());
        assertFalse(engine.getLockInfo("design", "feature/x", "P2").isLocked());
        assertTrue(engine.getLockInfo("design2", null, "P1").isLocked());
        assertTrue(engine.tryLock("design", null, "P1", "admin"));
    }

    @Test
    void releasingARepositoryWithoutLocksDoesNothing() {
        var engine = LockEngineImpl.create(workspaces.toFile(), "projects");

        engine.forceUnlockRepository("design");

        assertTrue(engine.tryLock("design", null, "P1", "admin"));
    }
}
