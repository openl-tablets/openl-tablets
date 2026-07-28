package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.WorkspaceUserImpl;

/**
 * The project lifecycle against the metainfo registry: open captures the snapshot, editing is tracked
 * in memory, close removes the project together with its record.
 *
 * @author Yury Molchan
 */
class RulesProjectLifecycleTest {

    private static final String PROJECT = "Example 1";

    @TempDir
    Path designRoot;

    @TempDir
    Path userDir;

    private FileSystemRepository designRepository;
    private MetainfoRegistry registry;
    private LocalRepository localRepository;

    @BeforeEach
    void init() throws IOException {
        // A plain file-system repository with a stub revision: openVersion requires the design
        // revision to exist.
        designRepository = new FileSystemRepository() {
            @Override
            protected String getVersion(Path file) {
                return "rev-1";
            }

            @Override
            protected String getVersion(String path) {
                return "rev-1";
            }
        };
        designRepository.setRoot(designRoot);
        designRepository.setId("design");
        designRepository.initialize();
        var fileData = new FileData();
        fileData.setName(PROJECT + "/rules/Main.xlsx");
        designRepository.save(fileData, stream("design content"));

        registry = MetainfoRegistry.open(userDir);
        localRepository = new LocalRepository(userDir, registry);
        localRepository.setId("design");
        localRepository.initialize();
    }

    @Test
    void openCapturesSnapshotAndCloseRemovesIt() throws Exception {
        var project = createProject();

        project.open();

        assertTrue(project.isOpened());
        assertFalse(project.isModified(), "A freshly opened project has no local changes.");
        var metainfo = registry.get(PROJECT);
        assertNotNull(metainfo);
        assertEquals("design", metainfo.repositoryId());
        var baseline = metainfo.files().get("/rules/Main.xlsx");
        assertNotNull(baseline, "Open must capture the baseline of every project file.");
        assertEquals("design content".length(), baseline.size());
        assertTrue(Files.exists(userDir.resolve(PROJECT).resolve("rules").resolve("Main.xlsx")));

        project.close();

        assertFalse(Files.exists(userDir.resolve(PROJECT)), "Close must delete the local copy.");
        assertNull(registry.get(PROJECT), "Close must delete the metainfo record.");
        assertFalse(registry.isDirty(PROJECT));
    }

    @Test
    void editingIsTrackedWithoutTouchingTheRecord() throws Exception {
        var project = createProject();
        project.open();
        var record = userDir.resolve(MetainfoRegistry.METAINFO_FOLDER).resolve(PROJECT + ".properties");
        var recordBytes = Files.readAllBytes(record);

        var change = new FileData();
        change.setName(PROJECT + "/rules/Main.xlsx");
        localRepository.save(change, stream("edited content!"));

        assertTrue(project.isModified(), "A file save must mark the project as locally changed.");
        assertArrayEqualsOnDisk(recordBytes, record);

        var reloaded = MetainfoRegistry.open(userDir);
        assertTrue(reloaded.isDirty(PROJECT),
                "The local changes must be reconstructed from the baselines after a restart.");
    }

    @Test
    void closeRemovesRelocatedEditHistory() throws Exception {
        var project = createProject();
        project.open();
        var history = userDir.resolve(".history").resolve(PROJECT).resolve("Main.xlsx");
        Files.createDirectories(history);
        Files.writeString(history.resolve("123_current"), "history entry");

        project.close();

        assertFalse(Files.exists(userDir.resolve(".history").resolve(PROJECT)),
                "The project edit history must leave the workspace together with the project.");
    }

    private RulesProject createProject() throws IOException {
        var designData = designRepository.check(PROJECT);
        return new RulesProject(new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localRepository,
                null,
                designRepository,
                designData,
                new DummyLockEngine());
    }

    private static ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertArrayEqualsOnDisk(byte[] expected, Path file) throws IOException {
        assertEquals(new String(expected, StandardCharsets.UTF_8), Files.readString(file),
                "Editing project files must not modify the metainfo record.");
    }
}
