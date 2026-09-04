package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.DataHeaderView;
import org.openl.studio.projects.model.tables.DataRowView;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.model.tables.TestView;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;

/**
 * A Data or Test table always carries a row of column titles between its field names and its data, so a table that
 * titles none of its columns would leave that row blank and end there.
 *
 * <p>Each test creates a table beside the one the fixture holds, reloads the project from disk, and reads the sheet
 * back — the in-memory grid is stale after a save.
 */
class DataTableIOTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsATestTableThatTitlesNoColumn() throws IOException {
        var project = writeProject("untitled-test");
        var untitled = TestView.builder()
                .name("HelloTest")
                .testedTableName("Hello")
                .headers(List.of(header("hour", null), header("_res_", null)))
                .rows(List.of(row(9, "Good Morning")))
                .build();

        // Writing it would leave the title row blank, ending the table at the field names and leaving the test
        // case out of it. The titles are the caller's to supply — they are business labels, not field names.
        assertThrows(BadRequestException.class, () -> create(project, "Cases", untitled));
    }

    @Test
    void keepsTheTitlesATestTableDeclares() throws IOException {
        var project = writeProject("titled-test");

        create(project, "Cases", TestView.builder()
                .name("HelloTest")
                .testedTableName("Hello")
                .headers(List.of(header("hour", "Hour"), header("_res_", "Expected")))
                .rows(List.of(row(9, "Good Morning")))
                .build());

        // Four rows, so the test case really is part of the table.
        assertEquals(List.of(
                Arrays.asList("Test Hello HelloTest", null),
                List.of("hour", "_res_"),
                List.of("Hour", "Expected"),
                List.of("9", "Good Morning")), sourceOf(project, "Cases"));
    }

    @Test
    void keepsTheTitlesADataTableDeclares() throws IOException {
        var project = writeProject("titled-data");

        create(project, "Cities", DataView.builder()
                .name("cities")
                .dataType("Greeting")
                .headers(List.of(header("hour", "Hour"), header("greeting", "Greeting")))
                .rows(List.of(row(9, "Good Morning")))
                .build());

        assertEquals(List.of(
                Arrays.asList("Data Greeting cities", null),
                List.of("hour", "greeting"),
                List.of("Hour", "Greeting"),
                List.of("9", "Good Morning")), sourceOf(project, "Cities"));
    }

    @Test
    void keepsAColumnUntitledWhileAnotherOneCarriesATitle() throws IOException {
        var project = writeProject("partly-titled");

        create(project, "Cities", DataView.builder()
                .name("cities")
                .dataType("Greeting")
                .headers(List.of(header("hour", "Hour"), header("greeting", null)))
                .rows(List.of(row(9, "Good Morning")))
                .build());

        // The title row carries the other column's title, so it is not blank and the untitled column stays as it
        // is — which is also what reading such a table back reports, so a read can always be written again.
        assertEquals(Arrays.asList("Hour", null), sourceOf(project, "Cities").get(2));
    }

    private static DataHeaderView header(String fieldName, String displayName) {
        return DataHeaderView.builder().fieldName(fieldName).displayName(displayName).build();
    }

    private static DataRowView row(Object... values) {
        return DataRowView.builder().values(List.of(values)).build();
    }

    /** Create a table on a sheet of its own, the way the create endpoint does. */
    private static <V extends TableView & EditableTableView> void create(Path project, String sheetName, V view) {
        var writer = new TableWritersFactory().getNewTableWriter(view, TableTestProjects.sheetGrid(project, sheetName));
        new TableWriterExecutor().executeWrite(writer, view);
    }

    /** The cells of the table written on {@code sheetName}, row by row, as plain text. */
    private static List<List<String>> sourceOf(Path project, String sheetName) {
        return TableTestProjects.rawSource(load(project, sheetName));
    }

    /** The one table written on {@code sheetName}. */
    private static IOpenLTable load(Path project, String sheetName) {
        for (TableSyntaxNode tsn : TableTestProjects.projectModel(project).getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            if (table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null
                    && sheetName.equals(tsn.getXlsSheetSourceCodeModule().getSheetName())) {
                return table;
            }
        }
        throw new IllegalStateException("No table resolved on sheet " + sheetName);
    }

    /** A single-module project holding the rules a test table can be written for. */
    private Path writeProject(String name) throws IOException {
        return TableTestProjects.writeProject(tempDir.resolve(name), name, "Rules", new String[][]{
                {"SmartRules String Hello (Integer hour)", null},
                {"Hour", "Greeting"},
                {"[0..12)", "Good Morning"}
        });
    }

}
