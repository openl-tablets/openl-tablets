package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that {@link DeletableHomeListener} leaves the application state a build can delete.
 *
 * <p>The read-only attribute forbids deletion on Windows only, so that is where the check has teeth: on a file
 * system that keeps no DOS attributes the file is deletable to begin with.
 */
class DeletableHomeListenerTest {

    private static final String CONFIGURED_NAME = "openl-test-${openl.start.milli}";

    @Test
    void aReadOnlyObjectOfARepositoryIsLeftDeletable(@TempDir Path target) throws IOException {
        var object = readOnlyObjectIn(target.resolve("openl-test-1785771797485"));

        endTheRunWith(target.resolve(CONFIGURED_NAME));

        assertDoesNotThrow(() -> Files.delete(object), "the build must be able to delete the object");
    }

    /**
     * A suite that starts the application more than once leaves a home per start, and the homes of earlier runs
     * are still there as well. The build deletes them all, so all of them are cleared.
     */
    @Test
    void everyHomeOfEveryRunIsCleared(@TempDir Path target) throws IOException {
        var earlier = readOnlyObjectIn(target.resolve("openl-test-1785771797485"));
        var later = readOnlyObjectIn(target.resolve("openl-test-1786712183725"));

        endTheRunWith(target.resolve(CONFIGURED_NAME));

        assertDoesNotThrow(() -> Files.delete(earlier), "the home of an earlier run must be deletable too");
        assertDoesNotThrow(() -> Files.delete(later), "the build must be able to delete the object");
    }

    @Test
    void aHomeTheRunNeverCreatedIsNoProblem(@TempDir Path target) {
        assertDoesNotThrow(() -> endTheRunWith(target.resolve("never-written").resolve(CONFIGURED_NAME)));
    }

    @Test
    void anUnsetHomeIsNoProblem() {
        var previous = System.getProperty("openl.home");
        System.clearProperty("openl.home");
        try {
            assertDoesNotThrow(() -> new DeletableHomeListener().testPlanExecutionFinished(null));
        } finally {
            restore(previous);
        }
    }

    private static Path readOnlyObjectIn(Path home) throws IOException {
        var objects = Files.createDirectories(home.resolve("repositories/design/.git/objects/69"));
        var object = Files.writeString(objects.resolve("031b62ce49bd499a58838522d4a9168d64b0f1"), "an object");
        var attributes = Files.getFileAttributeView(object, DosFileAttributeView.class);
        if (attributes != null) {
            attributes.setReadOnly(true);
        }
        return object;
    }

    private static void endTheRunWith(Path home) {
        var previous = System.setProperty("openl.home", home.toString());
        try {
            new DeletableHomeListener().testPlanExecutionFinished(null);
        } finally {
            restore(previous);
        }
    }

    private static void restore(String previous) {
        if (previous == null) {
            System.clearProperty("openl.home");
        } else {
            System.setProperty("openl.home", previous);
        }
    }
}
