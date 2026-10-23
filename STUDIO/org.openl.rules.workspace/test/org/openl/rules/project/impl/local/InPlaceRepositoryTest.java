package org.openl.rules.project.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FileData;

/**
 * A project edited in place leaves the registry of the user alone: it holds the copies of the workspace, and a copy
 * of another project may have the name of the folder.
 */
class InPlaceRepositoryTest {

    @TempDir
    Path userDir;

    @TempDir
    Path designRoot;

    private MetainfoRegistry registry;
    private InPlaceRepository repository;

    @BeforeEach
    void init() throws IOException {
        registry = MetainfoRegistry.open(userDir);
        // A copy in the workspace with the name of the folder edited in place.
        Files.createDirectories(userDir.resolve("Rating"));
        registry.save("Rating",
                new ProjectMetainfo("design", "other/Rating", null, "rev-1", "jdoe", 1751980000000L, 7L, null, Map.of()));
        Files.createDirectories(designRoot.resolve("Rating"));
        repository = new InPlaceRepository(designRoot, userDir, registry);
        repository.setId("design");
        repository.initialize();
    }

    @Test
    void aWriteIsNoChangeOfTheCopyOfTheWorkspace() throws IOException {
        var data = new FileData();
        data.setName("Rating/rules.xml");

        var saved = repository.save(data, new ByteArrayInputStream("<project/>".getBytes(StandardCharsets.UTF_8)));

        assertEquals("<project/>", Files.readString(designRoot.resolve("Rating/rules.xml")));
        assertNull(saved.getUniqueId(), "nothing is recorded about the file");
        assertFalse(registry.isDirty("Rating"));
        assertFalse(repository.getProjectState("Rating/rules.xml").isModified());
    }

    @Test
    void theEditHistoryStaysInTheWorkspace() {
        assertEquals(designRoot, repository.getRoot());
        assertEquals(userDir, repository.getWorkspaceFolder());
    }

    @Test
    void deletingTheFolderKeepsTheRecordOfTheCopy() throws IOException {
        var folder = new FileData();
        folder.setName("Rating");

        repository.delete(folder);

        assertFalse(Files.exists(designRoot.resolve("Rating")));
        assertNotNull(registry.get("Rating"));
    }
}
