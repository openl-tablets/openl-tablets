package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableKind;

/**
 * A new table of a Markdown module is a file named after the table, in the folder its sheet names.
 */
class MarkdownTableFilesTest {

    @TempDir
    Path folder;

    @BeforeEach
    void writeModule() throws IOException {
        Files.createDirectories(folder.resolve("rates/young"));
        Files.writeString(folder.resolve("premium.md"), "");
        Files.writeString(folder.resolve("Environment.md"), "");
    }

    @Test
    void tableGoesIntoTheModuleFolderForItsOwnNameOrNone() {
        assertEquals(folder.resolve("Discount.md"), fileFor(null, "Discount"));
        assertEquals(folder.resolve("Discount.md"), fileFor(" ", "Discount"));
        assertEquals(folder.resolve("Discount.md"), fileFor(folder.getFileName().toString(), "Discount"));
    }

    @Test
    void tableGoesIntoTheFolderOfTheModuleItsSheetNames() {
        assertEquals(folder.resolve("rates/Discount.md"), fileFor("rates", "Discount"));
        assertEquals(folder.resolve("rates/young/Discount.md"), fileFor("young", "Discount"));
        // A sheet the module does not have yet is a folder made at its root
        assertEquals(folder.resolve("bonus/Discount.md"), fileFor("bonus", "Discount"));
    }

    @Test
    void nameTakenByAnotherFileGetsANumber() throws IOException {
        Files.writeString(folder.resolve("premium1.md"), "");

        assertEquals(folder.resolve("premium2.md"), fileFor(null, "premium"));
        // A table named as a table of the whole module would be read as one at the root of the folder
        assertEquals(folder.resolve("Constants1.md"), fileFor(null, "Constants"));
        assertEquals(folder.resolve("rates/Constants.md"), fileFor("rates", "Constants"));
    }

    @Test
    void tableOfTheWholeModuleIsWrittenInTheFileOfItsFixedName() {
        assertEquals(folder.resolve("Properties.md"),
                MarkdownTableFiles.fileFor(folder, "rates", "Settings", TableKind.PROPERTIES));

        var error = assertThrows(BadRequestException.class,
                () -> MarkdownTableFiles.fileFor(folder, null, "Env", TableKind.ENVIRONMENT));

        assertEquals("openl.error.400.table.markdown.module-table.message", error.getErrorCode());
        assertArrayEquals(new Object[]{Path.of("Environment.md")}, error.getArgs());
    }

    @Test
    void sheetNamingAPathIsRefused() {
        for (var sheet : new String[]{"rates/young", "rates\\young", ".."}) {
            var error = assertThrows(BadRequestException.class, () -> fileFor(sheet, "Discount"));
            assertEquals("openl.error.400.table.sheet-name.invalid.message", error.getErrorCode());
        }
    }

    @Test
    void firstTableOfANewModuleGoesIntoTheFolderItsSheetNames() {
        assertEquals("rules/Rating/Premium.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", null, "Premium", TableKind.RULES));
        assertEquals("rules/Rating/Premium.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", "Premium", null));
        assertEquals("rules/Rating/rates/Premium.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "rates", "Premium", TableKind.RULES));
        assertEquals("rules/Rating/Constants.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "rates", "Rates", TableKind.CONSTANTS));
        var error = assertThrows(BadRequestException.class,
                () -> MarkdownTableFiles.firstFileOf("rules/Rating", "rates/young", "Premium", null));
        assertEquals("openl.error.400.table.sheet-name.invalid.message", error.getErrorCode());
    }

    private Path fileFor(String sheetName, String tableName) {
        return MarkdownTableFiles.fileFor(folder, sheetName, tableName, TableKind.RULES);
    }
}
