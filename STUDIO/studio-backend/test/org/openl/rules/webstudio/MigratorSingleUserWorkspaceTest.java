package org.openl.rules.webstudio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests of the single-user workspace move from the former {@code DEFAULT} name to the resolved user name.
 * EPBDS-16213 changed the single-user default from {@code DEFAULT} to the OS account.
 */
class MigratorSingleUserWorkspaceTest {

    @TempDir
    Path root;

    @Test
    void movesLegacyWorkspaceToResolvedName() throws IOException {
        var project = Files.createDirectories(root.resolve("DEFAULT").resolve("SoloProj"));
        Files.writeString(project.resolve("solo-wip.txt"), "work in progress");

        Migrator.migrateSingleUserWorkspace("single", root.toString(), "openl");

        assertFalse(Files.exists(root.resolve("DEFAULT")), "The legacy DEFAULT workspace is moved.");
        assertTrue(Files.exists(root.resolve("openl").resolve("SoloProj").resolve("solo-wip.txt")),
                "Uncommitted work is preserved under the resolved user name.");
    }

    @Test
    void keepsWorkspaceWhenNameStaysDefault() throws IOException {
        var project = Files.createDirectories(root.resolve("DEFAULT").resolve("SoloProj"));

        Migrator.migrateSingleUserWorkspace("single", root.toString(), "DEFAULT");

        assertTrue(Files.exists(project), "An install still using DEFAULT keeps its workspace in place.");
    }

    @Test
    void skipsWhenTargetWorkspaceExists() throws IOException {
        Files.createDirectories(root.resolve("DEFAULT").resolve("Legacy"));
        var fresh = Files.createDirectories(root.resolve("openl").resolve("Fresh"));

        Migrator.migrateSingleUserWorkspace("single", root.toString(), "openl");

        assertTrue(Files.exists(root.resolve("DEFAULT").resolve("Legacy")),
                "An existing target workspace is never overwritten.");
        assertTrue(Files.exists(fresh), "The existing target workspace is left intact.");
    }

    @Test
    void skipsWhenNoLegacyWorkspace() {
        Migrator.migrateSingleUserWorkspace("single", root.toString(), "openl");

        assertFalse(Files.exists(root.resolve("openl")), "Nothing is created without a legacy workspace.");
    }

    @Test
    void skipsWhenNotSingleMode() throws IOException {
        var project = Files.createDirectories(root.resolve("DEFAULT").resolve("SoloProj"));

        Migrator.migrateSingleUserWorkspace("multi", root.toString(), "openl");

        assertTrue(Files.exists(project), "In multi-user mode a DEFAULT folder is a real user and is not touched.");
    }

    @Test
    void toleratesBlankOrMissingConfiguration() throws IOException {
        Files.createDirectories(root.resolve("DEFAULT").resolve("SoloProj"));

        Migrator.migrateSingleUserWorkspace("single", null, "openl");
        Migrator.migrateSingleUserWorkspace("single", root.toString(), null);
        // A blank path must not fall through to Path.of(""), which resolves to the process working directory.
        Migrator.migrateSingleUserWorkspace("single", "", "openl");
        Migrator.migrateSingleUserWorkspace("single", "   ", "openl");
        Migrator.migrateSingleUserWorkspace("single", root.toString(), " ");

        assertTrue(Files.exists(root.resolve("DEFAULT")), "Blank or absent configuration is a no-op, not a failure.");
    }

    @Test
    void logsInsteadOfThrowingWhenMoveFails() throws IOException {
        Files.createDirectories(root.resolve("DEFAULT").resolve("SoloProj"));
        // A regular file where the target's parent directory is expected makes the move fail.
        Files.writeString(root.resolve("blocker"), "not a directory");

        // Must not throw: a failed migration cannot break startup.
        Migrator.migrateSingleUserWorkspace("single", root.toString(), "blocker/openl");

        assertTrue(Files.exists(root.resolve("DEFAULT")), "A failed move leaves the legacy workspace intact.");
    }

    @Test
    void skipsWhenUserNameTraversesOutOfRoot() throws IOException {
        var ws = Files.createDirectories(root.resolve("ws"));
        Files.createDirectories(ws.resolve("DEFAULT").resolve("SoloProj"));

        Migrator.migrateSingleUserWorkspace("single", ws.toString(), "../outside");

        assertTrue(Files.exists(ws.resolve("DEFAULT")), "A traversing user name must not move the workspace out of root.");
        assertFalse(Files.exists(root.resolve("outside")), "Nothing is created outside the workspace root.");
    }

    @Test
    void skipsWhenUserNameIsAbsolute(@TempDir Path other) throws IOException {
        Files.createDirectories(root.resolve("DEFAULT").resolve("SoloProj"));
        var absolute = other.resolve("hijack");

        Migrator.migrateSingleUserWorkspace("single", root.toString(), absolute.toString());

        assertTrue(Files.exists(root.resolve("DEFAULT")), "An absolute user name must not move the workspace out of root.");
        assertFalse(Files.exists(absolute), "Nothing is created at the absolute location.");
    }
}
