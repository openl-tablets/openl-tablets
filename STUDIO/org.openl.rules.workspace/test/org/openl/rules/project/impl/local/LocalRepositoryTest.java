package org.openl.rules.project.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.PropertiesUtils;

/**
 * Tests for the project link stored in the {@code .studioProps/.version} file.
 *
 * @author Yury Molchan
 */
class LocalRepositoryTest {

    private static final String PROJECT = "project1";

    @TempDir
    File root;

    private ProjectState projectState;

    @BeforeEach
    void init() {
        assertTrue(new File(root, PROJECT).mkdirs());
        projectState = new LocalRepository(root.toPath()).getProjectState(PROJECT);
    }

    @Test
    void linkIsFullWhenAuthorIsKnown() throws IOException {
        projectState.saveFileData("design", createFileData(new UserInfo("jdoe", "jdoe@email.to", "John Doe")));

        assertEquals("John Doe", readVersionFile().get("author"));
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

        assertNull(readVersionFile().get("author"));
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

    private static FileData createFileData(UserInfo author) {
        FileData fileData = new FileData();
        fileData.setName(PROJECT);
        fileData.setVersion("rev-1");
        fileData.setAuthor(author);
        fileData.setModifiedAt(new Date());
        fileData.setComment("Init");
        return fileData;
    }

    private LinkedHashMap<String, String> readVersionFile() throws IOException {
        Path versionFile = root.toPath().resolve(PROJECT).resolve(FolderHelper.PROPERTIES_FOLDER).resolve(".version");
        assertTrue(Files.exists(versionFile));
        var properties = new LinkedHashMap<String, String>();
        PropertiesUtils.load(versionFile, properties::put);
        return properties;
    }
}
