package org.openl.rules.project.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.util.PropertiesUtils;

/**
 * Tests for the project link stored in the metainfo registry record.
 *
 * @author Yury Molchan
 */
class LocalRepositoryTest {

    private static final String PROJECT = "project1";

    @TempDir
    File root;

    private LocalRepository repository;
    private ProjectState projectState;

    @BeforeEach
    void init() {
        assertTrue(new File(root, PROJECT).mkdirs());
        MetainfoRegistry registry = MetainfoRegistry.open(root.toPath());
        repository = new LocalRepository(root.toPath(), registry);
        projectState = repository.getProjectState(PROJECT);
    }

    @Test
    void linkIsFullWhenAuthorIsKnown() throws IOException {
        projectState.saveFileData("design", createFileData(new UserInfo("jdoe", "jdoe@email.to", "John Doe")));

        assertEquals("John Doe", readRecord().get("author"));
        FileData link = projectState.getFileData();
        assertNotNull(link);
        assertEquals("design", projectState.getRepositoryId());
        assertEquals("rev-1", link.getVersion());
        assertEquals("John Doe", link.getAuthor().getName());
    }

    @Test
    void linkSurvivesBlankAuthorName() throws IOException {
        // Regression for EPBDS-16228: a revision authored with a blank git ident must not leave
        // the checked out project unlinked, otherwise it shows up as a separate local project.
        projectState.saveFileData("design", createFileData(new UserInfo(null, "some@email.to", "")));

        assertNull(readRecord().get("author"));
        FileData link = projectState.getFileData();
        assertNotNull(link);
        assertEquals("design", projectState.getRepositoryId());
        assertEquals("rev-1", link.getVersion());
        assertNull(link.getAuthor());
    }

    @Test
    void linkSurvivesAbsentAuthor() {
        projectState.saveFileData("design", createFileData(null));

        FileData link = projectState.getFileData();
        assertNotNull(link);
        assertEquals("rev-1", link.getVersion());
        assertNull(link.getAuthor());
    }

    @Test
    void emptyFileDataIsNotSaved() {
        FileData fileData = createFileData(new UserInfo("jdoe"));
        fileData.setVersion(null);

        projectState.saveFileData("design", fileData);

        assertNull(projectState.getFileData());
        assertNull(projectState.getRepositoryId());
    }

    @Test
    void sameSizeSameTimeEditIsStillDetected() throws IOException {
        // An edit that accidentally matches the recorded baseline by size and modification time would
        // look unchanged after a restart. The repository must move the modification time forward.
        long baselineTime = System.currentTimeMillis() - 60_000;
        FileData data = new FileData();
        data.setName(PROJECT + "/rules/Main.xlsx");
        data.setModifiedAt(new Date(baselineTime));
        repository.save(data, new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8)));
        projectState.saveSnapshot("design",
                createFileData(null),
                Map.of("/rules/Main.xlsx", new ProjectMetainfo.FileBaseline(null, 5, baselineTime)));

        FileData saved = repository.save(data, new ByteArrayInputStream("54321".getBytes(StandardCharsets.UTF_8)));

        assertTrue(saved.getModifiedAt().getTime() > baselineTime,
                "The modification time must differ from the baseline after the edit.");
        assertTrue(projectState.isModified());
    }

    @Test
    void relinkPreservesLocalChanges() {
        projectState.notifyModified();
        assertTrue(projectState.isModified());

        projectState.saveFileData("design", createFileData(new UserInfo("jdoe")));

        assertTrue(projectState.isModified(), "Relinking is not a synchronization point.");
    }

    private static FileData createFileData(UserInfo author) {
        FileData fileData = new FileData();
        fileData.setName(PROJECT);
        fileData.setVersion("rev-1");
        fileData.setAuthor(author);
        fileData.setModifiedAt(new Date());
        fileData.setComment("Init");
        return fileData;
    }

    private LinkedHashMap<String, String> readRecord() throws IOException {
        Path record = root.toPath().resolve(MetainfoRegistry.METAINFO_FOLDER).resolve(PROJECT + ".properties");
        assertTrue(Files.exists(record));
        var properties = new LinkedHashMap<String, String>();
        PropertiesUtils.load(record, properties::put);
        return properties;
    }
}
