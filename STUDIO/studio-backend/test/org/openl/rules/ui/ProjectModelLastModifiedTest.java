package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A module changes with its file, and a module written as a folder with any file in it.
 */
class ProjectModelLastModifiedTest {

    @TempDir
    private Path project;

    @Test
    void workbookChangesWithItsFile() throws IOException {
        var workbook = Files.writeString(project.resolve("Main.xlsx"), "v1");
        Files.setLastModifiedTime(workbook, FileTime.fromMillis(1_000_000));

        assertEquals(1_000_000, ProjectModel.lastModified(workbook));
    }

    @Test
    void folderChangesWithAFileRewrittenInIt() throws IOException {
        var folder = Files.createDirectories(project.resolve("rules/Rating"));
        var table = Files.writeString(folder.resolve("premium.md"), "v1");
        Files.setLastModifiedTime(table, FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(folder, FileTime.fromMillis(1_000_000));
        var before = ProjectModel.lastModified(folder);

        // Rewritten in place: the folder keeps its own time
        Files.writeString(table, "v2");
        Files.setLastModifiedTime(table, FileTime.fromMillis(2_000_000));
        Files.setLastModifiedTime(folder, FileTime.fromMillis(1_000_000));

        assertNotEquals(before, ProjectModel.lastModified(folder));
        assertEquals(2_000_000, ProjectModel.lastModified(folder));
    }
}
