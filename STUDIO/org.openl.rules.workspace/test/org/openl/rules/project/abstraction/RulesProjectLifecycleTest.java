package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
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
        var recordFile = userDir.resolve(MetainfoRegistry.METAINFO_FOLDER).resolve(PROJECT + ".properties");
        var recordBytes = Files.readAllBytes(recordFile);

        var change = new FileData();
        change.setName(PROJECT + "/rules/Main.xlsx");
        localRepository.save(change, stream("edited content!"));

        assertTrue(project.isModified(), "A file save must mark the project as locally changed.");
        assertArrayEqualsOnDisk(recordBytes, recordFile);

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

    @Test
    void openingAnUnknownRevisionLeavesTheOpenedCopyUntouched() throws Exception {
        var versionedDesignRepository = new FileSystemRepository() {
            @Override
            protected String getVersion(Path file) {
                return "rev-1";
            }

            @Override
            protected String getVersion(String path) {
                return "rev-1";
            }

            @Override
            public Features supports() {
                return new FeaturesBuilder(this).setVersions(true).setFolders(true).setSupportsUniqueFileId(true).build();
            }
        };
        versionedDesignRepository.setRoot(designRoot.resolve("versioned"));
        versionedDesignRepository.setId("design");
        versionedDesignRepository.initialize();
        var fileData = new FileData();
        fileData.setName(PROJECT + "/rules/Main.xlsx");
        versionedDesignRepository.save(fileData, stream("design content"));
        var project = new RulesProject(new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localRepository,
                null,
                versionedDesignRepository,
                versionedDesignRepository.check(PROJECT),
                new DummyLockEngine());
        project.open();
        var localFile = userDir.resolve(PROJECT).resolve("rules").resolve("Main.xlsx");
        assertEquals("design content", Files.readString(localFile));

        var exception = assertThrows(ProjectException.class, () -> project.openVersion("rev-x"));

        assertEquals("Cannot open. Revision not found.", exception.getMessage());
        assertTrue(project.isOpened(), "The project must stay opened at the revision it was opened on.");
        assertEquals("rev-1", project.getHistoryVersion());
        assertEquals("design content", Files.readString(localFile),
                "A revision that cannot be opened must not touch the opened copy.");
        assertFalse(project.isModified(), "A refused open must leave no pending changes behind.");
    }

    @Test
    void designProjectNameDoesNotUseTheOpenedLocalFolderName() {
        var localData = new FileData();
        localData.setName("Pricing");
        var designData = new FileData();
        designData.setName("DESIGN/Pricing:0123456789");
        var project = new RulesProject(
                new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localRepository,
                localData,
                designRepository,
                designData,
                new DummyLockEngine());

        assertEquals("Pricing", project.getName());
        assertEquals("Pricing:0123456789", project.getDesignProjectName());
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
