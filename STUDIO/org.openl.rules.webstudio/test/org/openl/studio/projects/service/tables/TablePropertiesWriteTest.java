package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Verifies that a table's properties are written where they stand, without its body taking part.
 *
 * <p>Each test writes a project on disk, writes properties onto its table, and reads the sheet back — the grid the
 * table was read from is stale once the workbook is saved.
 */
class TablePropertiesWriteTest {

    @TempDir
    Path tempDir;

    private final SystemPropertiesService systemProperties = mock(SystemPropertiesService.class);
    private final TablePropertiesService service = new TablePropertiesServiceImpl(systemProperties);

    @Test
    void writesAPropertyOntoATableThatDeclaresNone() throws IOException {
        var project = rules("plain");

        service.write(table(project), List.of(new TableProperty("description", "Greets by the hour")));

        // The properties section is written above the body, which stays where it was.
        assertEquals(List.of(
                List.of("SmartRules String Hello (Integer hour)"),
                List.of("properties", "description", "Greets by the hour"),
                List.of("Hour", "Greeting"),
                List.of("[0..12)", "Good Morning")), source(project));
    }

    @Test
    void changesAPropertyTheTableAlreadyCarries() throws IOException {
        var project = withProperties("changed");

        service.write(table(project), List.of(new TableProperty("description", "Says hello")));

        assertEquals(List.of("properties", "description", "Says hello"), source(project).get(1));
    }

    @Test
    void takesAwayAPropertyGivenNoValue() throws IOException {
        var project = withProperties("removed");

        service.write(table(project), List.of(new TableProperty("description", null)));

        // The row the value sat on is gone; the table starts at its body again.
        assertEquals(List.of(
                List.of("SmartRules String Hello (Integer hour)"),
                List.of("Hour", "Greeting"),
                List.of("[0..12)", "Good Morning")), source(project));
    }

    @Test
    void leavesAlonePropertiesItIsNotToldAbout() throws IOException {
        var project = withProperties("kept");

        service.write(table(project), List.of(new TableProperty("tags", "greeting")));

        // The one it was told about is written; the one it was not still stands.
        assertEquals(Map.of("description", "Greets by the hour", "tags", "greeting"),
                service.read(table(project)).stream()
                        .collect(Collectors.toMap(TableProperty::name, TableProperty::value)));
    }

    @Test
    void readsADateFromTheTextItCrossesIn() throws IOException {
        var project = rules("dated");

        service.write(table(project), List.of(new TableProperty("effectiveDate", "2009-01-01")));

        // Written as a date rather than as the text it arrived as, so the engine reads it as one.
        assertEquals("2009-01-01", service.read(table(project)).getFirst().value());
    }

    @Test
    void recordsWhoEditedTheTableWhereTheInstallationRecordsIt() throws IOException {
        var project = rules("stamped");
        when(systemProperties.onEdit()).thenReturn(Map.of("modifiedBy", "jane"));

        service.write(table(project), List.of(new TableProperty("description", "Greets by the hour")));

        assertTrue(source(project).contains(List.of("properties", "modifiedBy", "jane")),
                "the edit should be recorded: " + source(project));
    }

    @Test
    void refusesAPropertyThisKindOfTableDoesNotAccept() throws IOException {
        var project = rules("unsuitable");
        var table = table(project);
        // A datatype's package is not something a rules table can be given.
        var unsuitable = List.of(new TableProperty("datatypePackage", "org.openl"));

        assertThrows(BadRequestException.class, () -> service.write(table, unsuitable));
    }

    @Test
    void refusesATableThatCarriesNoPropertiesAtAll() throws IOException {
        var project = TableTestProjects.writeProject(tempDir.resolve("environment"), "environment", "Env",
                new String[][]{{"Environment", null}, {"include", "Rules.xlsx"}});
        var table = table(project);
        var properties = List.of(new TableProperty("description", "Anything"));

        assertThrows(BadRequestException.class, () -> service.write(table, properties));
    }

    /** A single-module project holding one rules table. */
    private Path rules(String name) throws IOException {
        return TableTestProjects.writeProject(tempDir.resolve(name), name, "Rules", new String[][]{
                {"SmartRules String Hello (Integer hour)", null},
                {"Hour", "Greeting"},
                {"[0..12)", "Good Morning"}
        });
    }

    /** The same table, already declaring a property of its own. */
    private Path withProperties(String name) throws IOException {
        return TableTestProjects.writeProject(tempDir.resolve(name), name, "Rules", new String[][]{
                {"SmartRules String Hello (Integer hour)", null, null},
                {"properties", "description", "Greets by the hour"},
                {"Hour", "Greeting", null},
                {"[0..12)", "Good Morning", null}
        });
    }

    /** The one table of the project, read afresh from the workbook. */
    private static IOpenLTable table(Path project) {
        return new TableSyntaxNodeAdapter(TableTestProjects.projectModel(project).getAllTableSyntaxNodes().iterator()
                .next());
    }

    /** The table's cells, row by row, with the blank tail of each row left out. */
    private static List<List<String>> source(Path project) {
        return TableTestProjects.rawSource(table(project)).stream().map(TablePropertiesWriteTest::trimmed).toList();
    }

    /** One row without the blank cells a wider row leaves at its end. */
    private static List<String> trimmed(List<String> row) {
        var cells = new ArrayList<>(row);
        while (!cells.isEmpty() && cells.getLast() == null) {
            cells.removeLast();
        }
        return cells;
    }
}
