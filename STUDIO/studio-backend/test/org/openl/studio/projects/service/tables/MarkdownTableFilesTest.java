package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;

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
        // A module may be named with a dot its folder does not take: a sheet named after the module is its folder
        assertEquals(folder.resolve("Discount.md"),
                MarkdownTableFiles.fileFor(folder, "Rating.v2", "Rating.v2", "Discount", TableKind.RULES));
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
        assertEquals(folder.resolve("Constants.md"),
                MarkdownTableFiles.fileFor(folder, null, "rates", "Settings", TableKind.CONSTANTS));
        // A Properties table stands in the folder of its sheet, as one of a category takes the category of its folder
        assertEquals(folder.resolve("rates/Settings.md"),
                MarkdownTableFiles.fileFor(folder, null, "rates", "Settings", TableKind.PROPERTIES));

        var error = assertThrows(BadRequestException.class,
                () -> MarkdownTableFiles.fileFor(folder, null, null, "Env", TableKind.ENVIRONMENT));

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
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", null, "Premium", TableKind.RULES));
        assertEquals("rules/Rating/Premium.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", "Rating", "Premium", null));
        assertEquals("rules/Rating/rates/Premium.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", "rates", "Premium", TableKind.RULES));
        assertEquals("rules/Rating-v2/Coverage.md",
                MarkdownTableFiles.firstFileOf("rules/Rating-v2", "Rating.v2", "Rating.v2", "Coverage", null));
        assertEquals("rules/Rating/Constants.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", "rates", "Rates", TableKind.CONSTANTS));
        // A table named as a table of the module takes a number at the root, where that file is reserved
        assertEquals("rules/Rating/Environment1.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", null, "Environment", TableKind.DATATYPE));
        assertEquals("rules/Rating/rates/Constants.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", "rates", "Constants", TableKind.SPREADSHEET));
        // A character a file name cannot hold, and a path, are no part of the name of the file
        assertEquals("rules/Rating/a_b_c.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", null, "a:b/c", TableKind.RULES));
        // The folder of a sheet holds no character a name of OpenL Studio refuses
        assertEquals("rules/Rating/rates_ 50_/Premium.md",
                MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", " rates; 50%.", "Premium",
                        TableKind.RULES));
        var error = assertThrows(BadRequestException.class,
                () -> MarkdownTableFiles.firstFileOf("rules/Rating", "Rating", "rates/young", "Premium", null));
        assertEquals("openl.error.400.table.sheet-name.invalid.message", error.getErrorCode());
    }

    /** The file of a new table a write could not finish goes; a file not written yet is left alone. */
    @Test
    void fileOfANewTableIsTakenBackOnceWritten(@TempDir Path project) throws IOException {
        var model = greetings(project, false);
        var projectFiles = mock(ProjectFilesService.class);
        var rootFactory = mock(ProjectFileRootFactory.class);
        var root = mock(FileRoot.class);
        when(rootFactory.of(any())).thenReturn(root);
        var files = new MarkdownTableFiles(projectFiles, rootFactory);
        var grid = files.newFile(model, MarkdownTableFiles.folderOf(model.getModuleInfo()), null, "salute",
                TableKind.RULES);

        files.takeBack(model, grid);
        verify(projectFiles, never()).deleteResource(any(), any());

        Files.writeString(project.resolve("rules/Greetings/salute.md"), "---\ntable: Rules String salute()\n---\n");
        files.takeBack(model, grid);
        verify(projectFiles).deleteResource(root, "rules/Greetings/salute.md");
    }

    @Test
    void tableIsDeletedWithItsFile(@TempDir Path project) throws IOException {
        var model = greetings(project, true);
        var projectFiles = mock(ProjectFilesService.class);
        var rootFactory = mock(ProjectFileRootFactory.class);
        var root = mock(FileRoot.class);
        var rulesProject = mock(RulesProject.class);
        when(rootFactory.of(rulesProject)).thenReturn(root);

        new MarkdownTableFiles(projectFiles, rootFactory).delete(rulesProject, model.getModuleInfo(),
                TableTestProjects.table(model, "discount"));

        verify(projectFiles).deleteResource(root, "rules/Greetings/rates/discount.md");
    }

    @Test
    void lastTableOfAModuleStays(@TempDir Path project) throws IOException {
        var model = greetings(project, false);
        // A file of documentation is no table: the greeting is the last table of the module still
        Files.writeString(project.resolve("rules/Greetings/README.md"), "# Greetings\n");
        var projectFiles = mock(ProjectFilesService.class);
        var files = new MarkdownTableFiles(projectFiles, mock(ProjectFileRootFactory.class));
        var module = model.getModuleInfo();
        var greeting = TableTestProjects.table(model, "greeting");

        // A module folder with no file of a table does not compile
        var error = assertThrows(BadRequestException.class, () -> files.delete(mock(RulesProject.class), module,
                greeting));

        assertEquals("openl.error.400.table.markdown.last-table.message", error.getErrorCode());
        verify(projectFiles, never()).deleteResource(any(), any());
    }

    /** A compiled module of Markdown files: the greeting at its root and, when asked, a discount in a folder. */
    private static ProjectModel greetings(Path project, boolean withDiscount) throws IOException {
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Greetings</name>
                    <modules>
                        <module>
                            <name>Greetings</name>
                            <rules-root path="rules/Greetings"/>
                        </module>
                    </modules>
                </project>
                """);
        var folder = Files.createDirectories(project.resolve("rules/Greetings/rates"));
        Files.writeString(folder.resolveSibling("greeting.md"), """
                ---
                table: SmartRules String greeting(Integer hour)
                ---

                | Hour  | Greeting       |
                |-------|----------------|
                | 0-11  | Good Morning   |
                | 12-23 | Good Afternoon |
                """);
        if (withDiscount) {
            Files.writeString(folder.resolve("discount.md"), """
                    ---
                    table: SmartRules Double discount(Integer age)
                    ---

                    | Age   | Discount |
                    |-------|----------|
                    | 0-120 | 0.1      |
                    """);
        }
        return TableTestProjects.projectModel(project);
    }

    private Path fileFor(String sheetName, String tableName) {
        return MarkdownTableFiles.fileFor(folder, null, sheetName, tableName, TableKind.RULES);
    }
}
