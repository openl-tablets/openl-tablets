package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.AppendTarget;
import org.openl.studio.projects.model.tables.RawCellInput;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableProperty;
import org.openl.studio.projects.service.tables.SystemPropertiesService;
import org.openl.studio.projects.service.tables.TableCopyServiceImpl;
import org.openl.studio.projects.service.tables.TableCreatorService;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.TableVersionService;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * A table keeps its id when a write moves it, and the id a write answers is the one the next compile gives the
 * table.
 */
class TableIdAfterWriteTest {

    private static final String SHEET = "Main";

    @TempDir
    private Path dir;

    @Test
    void aTableTheWriteMovesKeepsItsIdAndItsNumber() throws IOException {
        // Two tables of one kind with no name share what they are, and are told apart by their order.
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Constants");
            TableTestProjects.row(sheet, 2, 1, "Integer", "A", "1");
            TableTestProjects.row(sheet, 4, 1, "Constants");
            TableTestProjects.row(sheet, 5, 1, "Integer", "B", "2");
        });
        var first = tableAt(model, 1);
        var second = tableAt(model, 4);
        var firstId = first.getId();
        var secondId = second.getId();
        assertNotEquals(firstId, secondId);

        // There is no room to grow the first table before the second, so the write moves it below the second.
        var writer = new RawTableWriter(first);
        writer.apply(List.of(new RawTableSourceAction.Append(new AppendTarget.Rows(List.of(
                row("Integer", "C", "3"), row("Integer", "D", "4"))))));

        assertTrue(first.getGridTable().getRegion().getTop() > 4, "the write moved the table");
        assertEquals(firstId, writer.getTableId());
        var compiled = TableTestProjects.projectModel(dir);
        assertEquals(firstId, tableHeadedBy(compiled, "A").getId(), "the moved table keeps its id");
        assertEquals(secondId, tableHeadedBy(compiled, "B").getId(), "and the other keeps its own");
    }

    @Test
    void aRenameAnswersTheIdTheNextCompileGivesTheTable() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Greeting");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
        });
        var table = tableAt(model, 1);
        var id = table.getId();

        var writer = new RawTableWriter(table);
        var source = new RawTableReader().read(table).source;
        writer.write(RawTableView.builder().source(List.of(
                List.of(cell("Datatype Salute"), cell(null)),
                source.get(1))).build());

        var renamed = writer.getTableId();
        assertNotEquals(id, renamed);
        assertEquals(tableAt(TableTestProjects.projectModel(dir), 1).getId(), renamed);
    }

    @Test
    void aNewTableIsNumberedAmongTheTablesOfItsModule() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Constants");
            TableTestProjects.row(sheet, 2, 1, "Integer", "A", "1");
        });
        var view = RawTableView.builder().source(List.of(
                List.of(cell("Constants"), cell(null), cell(null)),
                List.of(cell("Integer"), cell("B"), cell("2")))).build();
        var grid = TableTestProjects.sheetGrid(dir, SHEET);
        var writer = new TableWritersFactory(mock(TableFormatting.class)).getNewTableWriter(view, grid);
        writer.setModule(model.getXlsModuleNode());

        ((RawTableWriter) writer).write(view);

        var compiled = TableTestProjects.projectModel(dir);
        assertEquals(tableHeadedBy(compiled, "B").getId(), writer.getTableId());
        assertNotEquals(tableHeadedBy(compiled, "A").getId(), writer.getTableId());
    }

    @Test
    void aPropertyThatMovesTheTableKeepsItsId() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Greeting");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
            TableTestProjects.row(sheet, 4, 1, "Datatype Farewell");
            TableTestProjects.row(sheet, 5, 1, "String", "text");
        });
        var table = tableAt(model, 1);
        var id = table.getId();

        var gridTable = table.getGridTable();
        gridTable.edit();
        String saved;
        try {
            var editor = new TableEditorModel(table);
            editor.setProperty("description", "Says hello");
            editor.setProperty("tags", "greeting");
            saved = editor.save();
        } finally {
            gridTable.stopEditing();
        }

        assertTrue(table.getGridTable().getRegion().getTop() > 4, "the properties moved the table");
        assertEquals(id, saved);
        assertEquals(id, tableHeadedBy(TableTestProjects.projectModel(dir), "Datatype Greeting").getId());
    }

    @Test
    void aCopyKeepingTheNameIsNotTakenForTheVersionItRetires() throws IOException {
        // The table to copy has no room below for the properties of the version it will stand for.
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "SmartRules String greet(String name)");
            TableTestProjects.row(sheet, 2, 1, "Name", "Greeting");
            TableTestProjects.row(sheet, 3, 1, "\"a\"", "\"hello\"");
            TableTestProjects.row(sheet, 5, 1, "Datatype Farewell");
            TableTestProjects.row(sheet, 6, 1, "String", "text");
        });
        var source = tableAt(model, 1);
        var sourceId = source.getId();
        var copier = new TableCopyServiceImpl(mock(SystemPropertiesService.class), new TableVersionService(),
                mock(TableFormatting.class));
        var creator = new TableCreatorService(null, null, null, null, null, mock(TableFormatting.class), null);

        // The copy is written below the source, then the source takes the properties of the version it stood for,
        // and moves below the copy: the two share what they are, and are told apart by their order.
        var grid = creator.sheetGridModel(model, SHEET);
        var copyId = copier.copyInto(source, "greet", List.of(new TableProperty("version", "0.0.2")), grid,
                model.getTableSyntaxNodes());

        var compiled = TableTestProjects.projectModel(dir);
        assertEquals(copyId, versionOf(compiled, "0.0.2").getId(), "the copy is known by the id it was given");
        assertEquals(sourceId, versionOf(compiled, "0.0.1").getId(), "the retired version keeps its id");
        assertTrue(versionOf(compiled, "0.0.1").getGridTable().getRegion().getTop()
                > versionOf(compiled, "0.0.2").getGridTable().getRegion().getTop(), "the source moved below its copy");
    }

    private static TableSyntaxNode versionOf(ProjectModel model, String version) {
        return model.getAllTableSyntaxNodes().stream()
                .filter(node -> node.getTableProperties() != null
                        && version.equals(node.getTableProperties().getVersion()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No table of version " + version));
    }

    private static IOpenLTable tableAt(ProjectModel model, int row) {
        return model.getAllTableSyntaxNodes().stream()
                .filter(node -> node.getGridTable().getRegion().getTop() == row)
                .map(TableSyntaxNodeAdapter::new)
                .findFirst()
                .orElseThrow();
    }

    /** The table whose header or first body row starts with the given text. */
    private static TableSyntaxNode tableHeadedBy(ProjectModel model, String text) {
        return model.getAllTableSyntaxNodes().stream()
                .filter(node -> text.equals(node.getGridTable().getCell(0, 0).getStringValue())
                        || text.equals(node.getGridTable().getCell(1, 1).getStringValue()))
                .findFirst()
                .orElseThrow();
    }

    private static List<RawCellInput> row(Object... values) {
        return Arrays.stream(values).map(value -> new RawCellInput(value, null, null, null)).toList();
    }

    private static RawTableCell cell(Object value) {
        return RawTableCell.builder().value(value).build();
    }
}
