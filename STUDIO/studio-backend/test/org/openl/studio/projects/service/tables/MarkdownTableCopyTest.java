package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.excel.parser.markdown.MarkdownGrid;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.model.tables.TableProperty;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;

/**
 * A copy takes the format of its destination: a table of a Markdown module is copied into a workbook, and a table of a
 * workbook into a Markdown module, as values and merged cells.
 */
class MarkdownTableCopyTest {

    private static final String GREETING = """
            ---
            table: SmartRules String greeting(Integer hour)
            ---

            Greeting by the hour

            > Picks a greeting.

            <table>
            <tr><td>Hour</td><td>Greeting</td></tr>
            <tr><td>0-11</td><td>Good Morning</td></tr>
            <tr><td rowspan="2">12-23</td><td>Good Afternoon</td></tr>
            <tr><td>Good Evening</td></tr>
            </table>
            """;

    private static final String COVERED = "(covered)";

    private final TableCopyService copier = new TableCopyServiceImpl(mock(SystemPropertiesService.class),
            new TableVersionService(), mock(TableFormatting.class));
    private final ProjectFilesService projectFiles = mock(ProjectFilesService.class);
    private final MarkdownTableFiles markdownFiles = new MarkdownTableFiles(projectFiles,
            mock(ProjectFileRootFactory.class));

    @TempDir
    Path project;

    @BeforeEach
    void writeProject() throws IOException {
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
        Files.createDirectories(project.resolve("rules/Greetings"));
        Files.writeString(project.resolve("rules/Greetings/greeting.md"), GREETING);
        // A file the project is asked to create is written where it says
        doAnswer(invocation -> {
            try (InputStream content = invocation.getArgument(2)) {
                Files.write(project.resolve(invocation.<String>getArgument(1)), content.readAllBytes());
            }
            return null;
        }).when(projectFiles).createResource(any(), anyString(), any(), anyBoolean());
    }

    @Test
    void copyOfAMarkdownTableIsAFileOfItsOwnInItsModule() throws IOException {
        var model = TableTestProjects.projectModel(project);

        var copyId = copier.copyInto(TableTestProjects.table(model, "greeting"), "salutation", null,
                newFile(model, "salutation"), model.getTableSyntaxNodes());

        // The comments and the text after the table stay with the source: the copy is the table with its description
        assertEquals("""
                ---
                table: SmartRules String salutation(Integer hour)
                ---

                Greeting by the hour

                <table>
                <tr><td>Hour</td><td>Greeting</td></tr>
                <tr><td>0-11</td><td>Good Morning</td></tr>
                <tr><td rowspan="2">12-23</td><td>Good Afternoon</td></tr>
                <tr><td>Good Evening</td></tr>
                </table>
                """, text("rules/Greetings/salutation.md"));
        assertEquals(GREETING, text("rules/Greetings/greeting.md"));
        var compiled = TableTestProjects.projectModel(project);
        assertEquals(TableTestProjects.table(compiled, "salutation").getId(), copyId);
    }

    @Test
    void newVersionOfAMarkdownTableStandsTheSourceDownInItsFile() throws IOException {
        var model = TableTestProjects.projectModel(project);

        copier.copyInto(TableTestProjects.table(model, "greeting"), "greeting",
                List.of(new TableProperty("version", "2.0.0")), newFile(model, "greeting"),
                model.getTableSyntaxNodes());

        // The files of a module are compiled together, so the copy is the new version and the source steps aside
        var compiled = TableTestProjects.projectModel(project);
        var versions = compiled.getAllTableSyntaxNodes()
                .stream()
                .filter(table -> table.getUri().contains("greeting"))
                .map(TableSyntaxNode::getTableProperties)
                .toList();
        assertEquals(2, versions.size());
        var source = versions.stream().filter(properties -> !"2.0.0".equals(properties.getVersion())).findFirst()
                .orElseThrow();
        assertEquals(Boolean.FALSE, source.getActive(), "the replaced table is no longer active");
        var copy = versions.stream().filter(properties -> "2.0.0".equals(properties.getVersion())).findFirst()
                .orElseThrow();
        assertNotEquals(Boolean.FALSE, copy.getActive(), "the copy is the active version");
        assertEquals(List.of("greeting.md", "greeting1.md"),
                Files.list(project.resolve("rules/Greetings")).map(Path::getFileName).map(Path::toString).sorted()
                        .toList());
    }

    @Test
    void tableOfAWorkbookIsCopiedIntoAMarkdownFileAsValuesAndMerges(@TempDir Path workbookProject)
            throws IOException {
        var workbook = TableTestProjects.projectModel(workbookProject, "Rates", sheet -> {
            TableTestProjects.row(sheet, 1, 1, "SmartRules Double rate(Integer age, String state)");
            TableTestProjects.row(sheet, 2, 1, "Age", "State", "Rate");
            TableTestProjects.row(sheet, 3, 1, "0-17", "AL", "0.1");
            TableTestProjects.row(sheet, 4, 1, "", "AK", "0.2");
            TableTestProjects.row(sheet, 5, 1, "18-120", "AL", "0.3");
            sheet.addMergedRegion(new CellRangeAddress(3, 4, 1, 1));
        });
        var text = new String[1];
        var grid = MarkdownGrid.create("rate.md", written -> text[0] = written);

        copier.copyInto(TableTestProjects.table(workbook, "rate"), "rateCopy", null, grid, new TableSyntaxNode[0]);

        // The look of the workbook stays behind: a Markdown file keeps none
        assertEquals("""
                ---
                table: SmartRules Double rateCopy(Integer age, String state)
                ---

                <table>
                <tr><td>Age</td><td>State</td><td>Rate</td></tr>
                <tr><td rowspan="2">0-17</td><td>AL</td><td>0.1</td></tr>
                <tr><td>AK</td><td>0.2</td></tr>
                <tr><td>18-120</td><td>AL</td><td>0.3</td></tr>
                </table>
                """, text[0]);
    }

    @Test
    void tableOfAMarkdownFileIsCopiedIntoAWorkbook(@TempDir Path workbookProject) throws IOException {
        TableTestProjects.projectModel(workbookProject, "Rates", sheet -> TableTestProjects.row(sheet, 1, 1,
                "Data String notes"));
        var model = TableTestProjects.projectModel(project);
        var grid = TableTestProjects.sheetGrid(workbookProject, "Copies");

        copier.copyInto(TableTestProjects.table(model, "greeting"), "salutation", null, grid,
                new TableSyntaxNode[0]);

        var copy = TableTestProjects.table(TableTestProjects.projectModel(workbookProject), "salutation");
        // The workbook takes the values and the merged cells; a covered cell holds no value of its own. The last cell
        // of each row of the body reaches over the columns of the properties
        assertEquals(List.of(
                Arrays.asList("SmartRules String salutation(Integer hour)", COVERED, COVERED),
                Arrays.asList("properties", "description", "Greeting by the hour"),
                Arrays.asList("Hour", "Greeting", COVERED),
                Arrays.asList("0-11", "Good Morning", COVERED),
                Arrays.asList("12-23", "Good Afternoon", COVERED),
                Arrays.asList(COVERED, "Good Evening", COVERED)), cellsOf(copy));
    }

    private MarkdownGrid newFile(ProjectModel model, String tableName) {
        var folder = MarkdownTableFiles.folderOf(model.getModuleInfo());
        return markdownFiles.newFile(model, folder, null, tableName, TableKind.RULES);
    }

    /** The text of each cell of a table, a cell a merged range covers being marked as such. */
    private static List<List<String>> cellsOf(IOpenLTable table) {
        var grid = table.getGridTable();
        return IntStream.range(0, grid.getHeight())
                .mapToObj(row -> IntStream.range(0, grid.getWidth()).mapToObj(column -> {
                    var cell = grid.getCell(column, row);
                    var region = cell.getRegion();
                    var covered = region != null && (region.getLeft() != cell.getAbsoluteColumn()
                            || region.getTop() != cell.getAbsoluteRow());
                    return covered ? COVERED : cell.getStringValue();
                }).toList())
                .toList();
    }

    private String text(String path) throws IOException {
        return Files.readString(project.resolve(path), StandardCharsets.UTF_8);
    }
}
