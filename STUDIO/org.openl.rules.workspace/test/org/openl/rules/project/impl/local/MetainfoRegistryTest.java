package org.openl.rules.project.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.ProjectMetainfo.FileBaseline;

/**
 * Tests of the per-user metainfo registry: the record format, atomic writes, reconciliation with the
 * project folders, and the reconstruction of the local-changes state.
 *
 * @author Yury Molchan
 */
class MetainfoRegistryTest {

    private static final String PROJECT = "project1";

    @TempDir
    Path userDir;

    private MetainfoRegistry registry;

    @BeforeEach
    void init() {
        registry = MetainfoRegistry.open(userDir);
    }

    @Test
    void recordSurvivesReload() throws IOException {
        var trickyPath = "/rules/My Table = #1 : с кириллицей.xlsx";
        var metainfo = new ProjectMetainfo("design",
                "DESIGN/rules/Example 1 - Bank Rating",
                "main",
                "rev-1",
                "John Doe",
                1751980000000L,
                12345L,
                "Copied from Example 1",
                Map.of(trickyPath, new FileBaseline("9f3c1a7e", 54321L, 1751979000000L),
                        "/rules/Main.xlsx", new FileBaseline(null, 100L, 1751979000001L)));
        createProjectFolder();
        registry.save(PROJECT, metainfo);

        var reloaded = MetainfoRegistry.open(userDir).get(PROJECT);

        assertEquals(metainfo, reloaded);
        assertTrue(reloaded.hasRevision());
    }

    @Test
    void localProjectHasNoRevision() {
        registry.save(PROJECT, localMetainfo());

        var metainfo = registry.get(PROJECT);

        assertNotNull(metainfo);
        assertEquals("local", metainfo.repositoryId());
        assertFalse(metainfo.hasRevision());
        assertNull(metainfo.author());
    }

    @Test
    void saveLeavesNoTemporaryFiles() throws IOException {
        registry.save(PROJECT, localMetainfo());

        try (var files = Files.list(userDir.resolve(MetainfoRegistry.METAINFO_FOLDER))) {
            assertEquals(List.of(PROJECT + ".properties"),
                    files.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void interruptedWriteLeftoverIsCleanedOnLoad() throws IOException {
        createProjectFolder();
        registry.save(PROJECT, localMetainfo());
        var leftover = userDir.resolve(MetainfoRegistry.METAINFO_FOLDER).resolve(PROJECT + ".properties.tmp");
        Files.writeString(leftover, "partial write");

        var reloaded = MetainfoRegistry.open(userDir);

        assertFalse(Files.exists(leftover));
        assertNotNull(reloaded.get(PROJECT));
    }

    @Test
    void recordWithoutFolderIsDropped() throws IOException {
        createProjectFolder();
        registry.save(PROJECT, localMetainfo());
        registry.save("ghost", localMetainfo());

        var reloaded = MetainfoRegistry.open(userDir);

        assertNull(reloaded.get("ghost"));
        assertFalse(Files.exists(userDir.resolve(MetainfoRegistry.METAINFO_FOLDER).resolve("ghost.properties")));
        assertNotNull(reloaded.get(PROJECT), "The record with an existing folder must survive.");
    }

    @Test
    void folderWithoutRecordIsDeleted() throws IOException {
        Files.createDirectories(userDir.resolve("stray").resolve("rules"));
        Files.writeString(userDir.resolve("stray").resolve("rules").resolve("Main.xlsx"), "data");

        MetainfoRegistry.open(userDir);

        assertFalse(Files.exists(userDir.resolve("stray")));
    }

    @Test
    void serviceFoldersAreNotProjects() throws IOException {
        Files.createDirectories(userDir.resolve(".history").resolve(PROJECT));

        var reloaded = MetainfoRegistry.open(userDir);

        assertTrue(Files.exists(userDir.resolve(".history")), "Dot folders are service folders, not projects.");
        assertEquals(List.of(), List.copyOf(reloaded.projects()));
    }

    @Test
    void corruptRecordDropsRecordAndFolder() throws IOException {
        createProjectFolder();
        var metainfoDir = userDir.resolve(MetainfoRegistry.METAINFO_FOLDER);
        Files.createDirectories(metainfoDir);
        Files.writeString(metainfoDir.resolve(PROJECT + ".properties"), "no-repository-id=true");

        var reloaded = MetainfoRegistry.open(userDir);

        assertNull(reloaded.get(PROJECT));
        assertFalse(Files.exists(metainfoDir.resolve(PROJECT + ".properties")));
        assertFalse(Files.exists(userDir.resolve(PROJECT)));
    }

    @Test
    void unsupportedFormatVersionIsCorrupt() throws IOException {
        createProjectFolder();
        var metainfoDir = userDir.resolve(MetainfoRegistry.METAINFO_FOLDER);
        Files.createDirectories(metainfoDir);
        Files.writeString(metainfoDir.resolve(PROJECT + ".properties"),
                "format-version=999\nrepository-id=design\n");

        var reloaded = MetainfoRegistry.open(userDir);

        assertNull(reloaded.get(PROJECT));
        assertFalse(Files.exists(userDir.resolve(PROJECT)));
    }

    @Test
    void untouchedProjectIsClean() throws IOException {
        var metainfo = saveProjectWithFile("data");

        var reloaded = MetainfoRegistry.open(userDir);

        assertEquals(metainfo, reloaded.get(PROJECT));
        assertFalse(reloaded.isDirty(PROJECT));
    }

    @Test
    void changedFileContentMakesProjectDirty() throws IOException {
        saveProjectWithFile("data");
        Files.writeString(projectFile(), "changed data");

        assertTrue(MetainfoRegistry.open(userDir).isDirty(PROJECT));
    }

    @Test
    void touchedFileMakesProjectDirty() throws IOException {
        var metainfo = saveProjectWithFile("data");
        var baseline = metainfo.files().values().iterator().next();
        Files.setLastModifiedTime(projectFile(), FileTime.fromMillis(baseline.modifiedAt() + 1000));

        assertTrue(MetainfoRegistry.open(userDir).isDirty(PROJECT));
    }

    @Test
    void addedFileMakesProjectDirty() throws IOException {
        saveProjectWithFile("data");
        Files.writeString(userDir.resolve(PROJECT).resolve("added.txt"), "new");

        assertTrue(MetainfoRegistry.open(userDir).isDirty(PROJECT));
    }

    @Test
    void deletedFileMakesProjectDirty() throws IOException {
        saveProjectWithFile("data");
        Files.delete(projectFile());

        assertTrue(MetainfoRegistry.open(userDir).isDirty(PROJECT));
    }

    @Test
    void snapshotSaveResetsDirtyState() {
        registry.markDirty(PROJECT);
        assertTrue(registry.isDirty(PROJECT));

        registry.save(PROJECT, localMetainfo());

        assertFalse(registry.isDirty(PROJECT));
    }

    @Test
    void removeDropsRecordAndDirtyState() throws IOException {
        registry.save(PROJECT, localMetainfo());
        registry.markDirty(PROJECT);

        registry.remove(PROJECT);

        assertNull(registry.get(PROJECT));
        assertFalse(registry.isDirty(PROJECT));
        assertFalse(Files
                .exists(userDir.resolve(MetainfoRegistry.METAINFO_FOLDER).resolve(PROJECT + ".properties")));
    }

    @Test
    void repeatedSaveOverwritesRecord() throws IOException {
        createProjectFolder();
        registry.save(PROJECT, localMetainfo());

        registry.save(PROJECT, new ProjectMetainfo("design", null, "main", "rev-2", null, 1L, 2L, null, Map.of()));

        var reloaded = MetainfoRegistry.open(userDir).get(PROJECT);
        assertNotNull(reloaded);
        assertEquals("design", reloaded.repositoryId());
        assertEquals("rev-2", reloaded.version());
    }

    @Test
    void renameMovesRecordAndLocalChanges() throws IOException {
        createProjectFolder();
        registry.save(PROJECT, localMetainfo());
        registry.markDirty(PROJECT);

        registry.rename(PROJECT, "renamed");

        assertNull(registry.get(PROJECT));
        assertNotNull(registry.get("renamed"));
        assertFalse(registry.isDirty(PROJECT));
        assertTrue(registry.isDirty("renamed"), "The local-changes state must move with the record.");
        assertFalse(Files
                .exists(userDir.resolve(MetainfoRegistry.METAINFO_FOLDER).resolve(PROJECT + ".properties")));
        assertTrue(Files
                .exists(userDir.resolve(MetainfoRegistry.METAINFO_FOLDER).resolve("renamed.properties")));
    }

    @Test
    void renameFailsForUnregisteredProject() {
        // The caller relies on the failure to roll the folder rename back, otherwise the folder and
        // the record end up under different names and the reconciliation drops both.
        assertThrows(IllegalStateException.class, () -> registry.rename("ghost", "renamed"));
    }

    @Test
    void renameRejectsRegisteredDestination() throws IOException {
        createProjectFolder();
        registry.save(PROJECT, localMetainfo());
        registry.save("occupied", localMetainfo());

        assertThrows(IllegalStateException.class, () -> registry.rename(PROJECT, "occupied"));
        assertNotNull(registry.get(PROJECT), "A failed rename must keep the source record.");
        assertNotNull(registry.get("occupied"));
    }

    @Test
    void relinkPreservesBaselinesAndLocalChanges() throws IOException {
        var metainfo = saveProjectWithFile("data");
        registry.markDirty(PROJECT);

        registry.relink(PROJECT, new ProjectMetainfo("local", null, null, "rev-2", null, 2L, 3L, null, Map.of()));

        var relinked = registry.get(PROJECT);
        assertNotNull(relinked);
        assertEquals("local", relinked.repositoryId());
        assertEquals("rev-2", relinked.version());
        assertEquals(metainfo.files(), relinked.files(), "Relinking must keep the recorded baselines.");
        assertTrue(registry.isDirty(PROJECT), "Relinking is not a synchronization point.");
    }

    @Test
    void uniqueIdIsReturnedForUnchangedFile() throws IOException {
        saveProjectWithFile("data");
        var baseline = registry.get(PROJECT).files().get("/rules/Main.xlsx");

        assertEquals("rev-file-1",
                registry.uniqueId(PROJECT, "/rules/Main.xlsx", baseline.size(), baseline.modifiedAt()));
        assertNull(registry.uniqueId(PROJECT, "/rules/Main.xlsx", baseline.size() + 1, baseline.modifiedAt()),
                "A size mismatch means the file is changed and its revision is unknown.");
        assertNull(registry.uniqueId(PROJECT, "/rules/Main.xlsx", baseline.size(), baseline.modifiedAt() + 1),
                "A time mismatch means the file is changed and its revision is unknown.");
        assertNull(registry.uniqueId(PROJECT, "/rules/Other.xlsx", 1, 1),
                "A file without a baseline has no known revision.");
        assertNull(registry.uniqueId("unknown", "/rules/Main.xlsx", baseline.size(), baseline.modifiedAt()),
                "An unregistered project has no baselines.");
    }

    private static ProjectMetainfo localMetainfo() {
        return new ProjectMetainfo("local", null, null, null, null, null, null, null, Map.of());
    }

    private void createProjectFolder() throws IOException {
        Files.createDirectories(userDir.resolve(PROJECT));
    }

    private ProjectMetainfo saveProjectWithFile(String content) throws IOException {
        var file = projectFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        var metainfo = new ProjectMetainfo("design", null, "main", "rev-1", null, 1751980000000L,
                (long) content.length(), null,
                Map.of("/rules/Main.xlsx",
                        new FileBaseline("rev-file-1", Files.size(file),
                                Files.getLastModifiedTime(file).toMillis())));
        registry.save(PROJECT, metainfo);
        return metainfo;
    }

    private Path projectFile() {
        return userDir.resolve(PROJECT).resolve("rules").resolve("Main.xlsx");
    }
}
