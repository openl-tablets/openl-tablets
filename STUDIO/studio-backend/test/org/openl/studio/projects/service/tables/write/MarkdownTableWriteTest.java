package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.excel.parser.markdown.MarkdownWriteException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.studio.projects.model.tables.DeleteTarget;
import org.openl.studio.projects.model.tables.InsertTarget;
import org.openl.studio.projects.model.tables.MergeTarget;
import org.openl.studio.projects.model.tables.RawCellInput;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.UnmergeTarget;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * The table editor writes a table of a Markdown module into its file: the cells, the rows, the merged cells and the
 * properties, each change touching only its lines of the file.
 */
class MarkdownTableWriteTest {

    private static final String GREETING = """
            ---
            table: SmartRules String greeting(Integer hour)
            description: Greeting by the hour
            ---

            Picks a greeting.

            | Hour  | Greeting       |
            |-------|----------------|
            | 0-11  | Good Morning   |
            | 12-17 | Good Afternoon |
            | 18-23 | Good Evening   |
            """;

    /** The greeting with the afternoon and the evening under one merged hour. */
    private static final String MERGED = GREETING.replace("description: Greeting by the hour\n",
                    "description: Greeting by the hour\nmerges: [A3:A4]\n")
            .replace("| 18-23 | Good Evening   |", "|       | Good Evening   |");

    @TempDir
    Path project;

    private Path file;

    @BeforeEach
    void writeProject() throws IOException {
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Greetings</name>
                    <modules>
                        <module>
                            <rules-root path="rules/Greetings"/>
                        </module>
                    </modules>
                </project>
                """);
        file = project.resolve("rules/Greetings/greeting.md");
        Files.createDirectories(file.getParent());
        Files.writeString(file, GREETING, StandardCharsets.UTF_8);
    }

    @Test
    void changedValueRewritesItsRowAlone() throws IOException {
        var table = load("greeting");
        var id = table.getId();
        var writer = new RawTableWriter(table);

        // Table rows: the header, the description, the first row of the body, then the hours
        writer.apply(List.of(new RawTableSourceAction.Update(new UpdateTarget.Cell(3, 1, "Morning"))));

        assertEquals(GREETING.replace("| 0-11  | Good Morning   |", "| 0-11  | Morning        |"), written());
        assertEquals(id, writer.getTableId(), "the table is still what it was");
    }

    @Test
    void renamedTableIsKnownByTheIdItsModuleGivesIt() {
        var table = load("greeting");
        var id = table.getId();
        var writer = new RawTableWriter(table);

        writer.apply(List.of(new RawTableSourceAction.Update(new UpdateTarget.Cell(0, 0,
                "SmartRules String salutation(Integer hour)"))));

        assertNotEquals(id, writer.getTableId());
        assertEquals(load("salutation").getId(), writer.getTableId());
    }

    @Test
    void insertedRowLeavesTheOtherRowsAsTheyAre() throws IOException {
        // The table is as wide as its properties, which take three columns; the body keeps its two
        apply(new RawTableSourceAction.Insert(new InsertTarget.Rows(4, List.of(row("11-12", "Lunch", null)))));

        // The editor writes the range as it was typed, as it writes it into a workbook
        assertEquals(GREETING.replace("| 12-17 |", "| 11-12 | Lunch          |\n| 12-17 |"), written());
    }

    @Test
    void mergedCellsAreWrittenIntoTheFrontMatter() throws IOException {
        apply(new RawTableSourceAction.Update(new UpdateTarget.Cell(5, 0, null)),
                new RawTableSourceAction.Merge(new MergeTarget.Cells(4, 0, 2, 1)));

        assertEquals(GREETING.replace("description: Greeting by the hour\n",
                        "description: Greeting by the hour\nmerges: [A3:A4]\n")
                .replace("| 18-23 | Good Evening   |", "|       | Good Evening   |"), written());
    }

    @Test
    void unmergedCellsLeaveTheFrontMatter() throws IOException {
        Files.writeString(file, MERGED, StandardCharsets.UTF_8);

        apply(new RawTableSourceAction.Unmerge(new UnmergeTarget.Cells(4, 0)));

        assertEquals(MERGED.replace("merges: [A3:A4]\n", ""), written());
    }

    @Test
    void removedRowShrinksTheMergeItRanThrough() throws IOException {
        Files.writeString(file, MERGED, StandardCharsets.UTF_8);

        apply(new RawTableSourceAction.Delete(new DeleteTarget.Rows(5, 1)));

        // A merge left over a single cell is a merge no longer
        assertEquals(MERGED.replace("merges: [A3:A4]\n", "").replace("|       | Good Evening   |\n", ""), written());
    }

    @Test
    void excelFormulaIsRefusedAndTheFileStaysAsItWas() throws IOException {
        var writer = new RawTableWriter(load());
        var update = List.<RawTableSourceAction>of(new RawTableSourceAction.Update(new UpdateTarget.Cell(3, 1, null,
                null, "=B2*C2")));

        // A text starting with = is an OpenL formula, so an Excel formula has no text to be written as
        var error = assertThrows(MarkdownWriteException.class, () -> writer.apply(update));

        assertEquals("""
                The Excel formula '=B2*C2' cannot be written in a Markdown table, which holds values and OpenL \
                formulas""", error.getMessage());
        assertEquals(GREETING, written());
    }

    @Test
    void propertyIsWrittenIntoTheFrontMatter() throws IOException {
        var table = load();
        var grid = table.getGridTable();
        grid.edit();
        try {
            var editor = new TableEditorModel(table);
            editor.setProperty("tags", "greeting");
            editor.save();
        } finally {
            grid.stopEditing();
        }

        // The table editor puts a new property first, as in a workbook
        assertEquals(GREETING.replace("description: Greeting by the hour\n",
                "tags: greeting\ndescription: Greeting by the hour\n"), written());
    }

    private void apply(RawTableSourceAction... actions) {
        new RawTableWriter(load()).apply(Arrays.asList(actions));
    }

    private String written() throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private IOpenLTable load() {
        return load("greeting");
    }

    private IOpenLTable load(String name) {
        return TableTestProjects.projectModel(project)
                .getAllTableSyntaxNodes()
                .stream()
                .map(TableSyntaxNodeAdapter::new)
                .filter(table -> table.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static List<RawCellInput> row(Object... values) {
        return Arrays.stream(values).map(value -> new RawCellInput(value, null, null, null)).toList();
    }
}
