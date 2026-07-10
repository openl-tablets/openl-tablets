package org.openl.rules.webstudio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;

/**
 * Tests of the one-time migration of legacy {@code .studioProps} workspaces to the metainfo registry.
 *
 * @author Yury Molchan
 */
class MigratorWorkspaceTest {

    @TempDir
    Path workspacesRoot;

    private Path userDir;

    @BeforeEach
    void init() throws IOException {
        userDir = workspacesRoot.resolve("jdoe");
        Files.createDirectories(userDir);
    }

    @Test
    void fullLegacyMetainfoIsConverted() throws IOException {
        Path project = createProject("Example 1");
        Path studioProps = project.resolve(".studioProps");
        Files.createDirectories(studioProps);
        Files.writeString(studioProps.resolve(".version"), """
                repository-id=design
                path-in-repository=DESIGN/rules/Example 1
                version=rev-42
                branch=main
                author=John Doe
                modified-at=2026-07-01
                modified-at-long=1751980000000
                size=12345
                comment=Copied from Example 1
                """);
        Files.createFile(studioProps.resolve(".modified"));
        Path fileProperties = studioProps.resolve("file-properties").resolve("rules");
        Files.createDirectories(fileProperties);
        Files.writeString(fileProperties.resolve("Main.xlsx"), """
                unique-id=9f3c1a7e
                modified=true
                size=54321
                modified-at-long=1751979000000
                """);
        Files.createDirectories(project.resolve(".history").resolve("Main.xlsx"));

        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);

        assertFalse(Files.exists(studioProps), "The legacy .studioProps folder must be deleted.");
        assertFalse(Files.exists(project.resolve(".history")), "The in-project edit history must be deleted.");
        ProjectMetainfo metainfo = MetainfoRegistry.open(userDir).get("Example 1");
        assertNotNull(metainfo);
        assertEquals("design", metainfo.repositoryId());
        assertEquals("DESIGN/rules/Example 1", metainfo.pathInRepository());
        assertEquals("main", metainfo.branch());
        assertEquals("rev-42", metainfo.version());
        assertEquals("John Doe", metainfo.author());
        assertEquals(1751980000000L, metainfo.modifiedAt());
        assertEquals(12345L, metainfo.size());
        assertEquals("Copied from Example 1", metainfo.comment());
        assertTrue(metainfo.hasRevision());
        var baseline = metainfo.files().get("/rules/Main.xlsx");
        assertNotNull(baseline);
        assertEquals("9f3c1a7e", baseline.uniqueId());
        assertEquals(54321L, baseline.size());
        assertEquals(1751979000000L, baseline.modifiedAt());
    }

    @Test
    void folderWithoutMetainfoIsDeletedAtFirstLoad() throws IOException {
        Path project = createProject("Stray");

        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);

        assertTrue(Files.exists(project), "Migration itself does not delete folders.");
        var registry = MetainfoRegistry.open(userDir);
        assertNull(registry.get("Stray"), "A folder without a repository link gets no record.");
        assertFalse(Files.exists(project), "The registry-first reconciliation deletes the unlinked folder.");
    }

    @Test
    void versionWithoutRepositoryLinkGetsNoRecord() throws IOException {
        Path project = createProject("NoLink");
        Path studioProps = project.resolve(".studioProps");
        Files.createDirectories(studioProps);
        Files.writeString(studioProps.resolve(".version"), """
                version=rev-1
                modified-at-long=1751980000000
                """);

        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);

        assertNull(MetainfoRegistry.open(userDir).get("NoLink"),
                "A record without the source repository cannot be restored.");
        assertFalse(Files.exists(project));
    }

    @Test
    void deprecatedDateFormatIsDropped() throws IOException {
        Path studioProps = createProject("OldDate").resolve(".studioProps");
        Files.createDirectories(studioProps);
        Files.writeString(studioProps.resolve(".version"), """
                repository-id=design
                version=rev-1
                author=jdoe
                modified-at=2024-01-01
                """);

        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);

        ProjectMetainfo metainfo = MetainfoRegistry.open(userDir).get("OldDate");
        assertNotNull(metainfo);
        assertEquals("design", metainfo.repositoryId());
        assertEquals("rev-1", metainfo.version());
        assertNull(metainfo.modifiedAt(), "The deprecated date-only format is not supported anymore.");
        assertFalse(metainfo.hasRevision());
    }

    @Test
    void unparsableNumbersAreTolerated() throws IOException {
        Path studioProps = createProject("Broken").resolve(".studioProps");
        Path fileProperties = studioProps.resolve("file-properties");
        Files.createDirectories(fileProperties);
        Files.writeString(studioProps.resolve(".version"), """
                repository-id=design
                version=rev-1
                modified-at-long=1751980000000
                size=huge
                """);
        Files.writeString(fileProperties.resolve("Main.xlsx"), """
                unique-id=9f3c1a7e
                size=big
                modified-at-long=1751979000000
                """);

        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);

        ProjectMetainfo metainfo = MetainfoRegistry.open(userDir).get("Broken");
        assertNotNull(metainfo);
        assertTrue(metainfo.hasRevision());
        assertNull(metainfo.size());
        assertTrue(metainfo.files().isEmpty(),
                "A baseline without a numeric size is skipped, so the file is later detected as changed.");
    }

    @Test
    void serviceFoldersAreNotTouched() throws IOException {
        Path locks = workspacesRoot.resolve(".locks").resolve("design").resolve("Example 1");
        Files.createDirectories(locks);
        Files.writeString(locks.resolve("ready.lock"), "user=jdoe");
        createProject("Plain");

        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);

        assertTrue(Files.exists(locks.resolve("ready.lock")), "The shared .locks storage is not a user workspace.");
        assertFalse(Files.exists(workspacesRoot.resolve(".locks").resolve(".metainfo")));
    }

    @Test
    void migrationIsIdempotent() throws IOException {
        Path studioProps = createProject("Twice").resolve(".studioProps");
        Files.createDirectories(studioProps);
        Files.writeString(studioProps.resolve(".version"), """
                repository-id=design
                version=rev-1
                modified-at-long=1751980000000
                """);

        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);
        Migrator.migrateUserWorkspacesToMetainfoRegistry(workspacesRoot);

        ProjectMetainfo metainfo = MetainfoRegistry.open(userDir).get("Twice");
        assertNotNull(metainfo);
        assertEquals("design", metainfo.repositoryId(),
                "A repeated migration must not degrade the record to a local project.");
    }

    private Path createProject(String name) throws IOException {
        Path project = userDir.resolve(name);
        Files.createDirectories(project.resolve("rules"));
        Files.writeString(project.resolve("rules").resolve("Main.xlsx"), "content");
        return project;
    }
}
