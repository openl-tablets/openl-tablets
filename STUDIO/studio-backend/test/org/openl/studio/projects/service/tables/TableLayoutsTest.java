package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;

/** How a table is laid out, whichever way round it is written. */
class TableLayoutsTest {

    private static final String[][] MODULE = {
            {"SimpleRules String greeting(String name)"},
            {"name", "greeting"},
            {"John", "Hi, John"},
            {"Mary", "Hello, Mary"},
            {},
            // Cases down the rows: the shape the Editor numbered.
            {"Test greeting downTheRowsTest"},
            {"name", "_res_"},
            {"Name", "Greeting"},
            {"John", "Hi, John"},
            {"Mary", "Hello, Mary"},
            {},
            // Cases across the columns: the shape the Editor left unnumbered. The column after the field
            // names holds the titles, as the row after them does in a table written the usual way round.
            {"Test greeting acrossTheColumnsTest"},
            {"name", "Name", "John", "Mary"},
            {"_res_", "Greeting", "Hi, John", "Hello, Mary"},
            {},
            // One case, written the same way: the other shape the Editor left unnumbered.
            {"Test greeting oneCaseTest"},
            {"name", "Name", "John"},
            {"_res_", "Greeting", "Hi, John"},
    };

    private ProjectModel model;

    @BeforeEach
    void writeModule(@TempDir Path dir) throws Exception {
        TableTestProjects.writeProject(dir, "Rules", "Rules", MODULE);
        model = TableTestProjects.projectModel(dir);
    }

    @Test
    void readsATableWrittenTheUsualWayRound() {
        var layout = TableLayouts.of(model, table("downTheRowsTest"));

        assertNotNull(layout);
        assertFalse(layout.transposed(), "a line of this table's data is a row");
        // The header line, the field names and the column titles come first; the cases follow.
        assertEquals(3, layout.firstDataLine());
        assertNull(layout.caseLines(), "every case takes one line");
    }

    @Test
    void readsTheLinesOfCasesWrittenInSeveralRows(@TempDir Path dir) throws Exception {
        var multiRow = TableTestProjects.projectModel(dir, "Rules", sheet -> {
            writePolicyTypes(sheet);
            // The first case defines two vehicles, one per row; the second one.
            TableTestProjects.row(sheet, 11, 1, "Test count downTheRowsTest");
            TableTestProjects.row(sheet, 12, 1, "pol.name", "pol.vehicles.name", "_res_");
            TableTestProjects.row(sheet, 13, 1, "Policy", "Vehicle", "Count");
            TableTestProjects.row(sheet, 14, 1, "P1", "A", "2");
            TableTestProjects.row(sheet, 15, 2, "B");
            TableTestProjects.row(sheet, 16, 1, "P2", "C", "1");
            TableTestProjects.merge(sheet, 14, 15, 1, 1);
            TableTestProjects.merge(sheet, 14, 15, 3, 3);
        });

        var layout = TableLayouts.of(multiRow, TableTestProjects.table(multiRow, "downTheRowsTest"));

        assertNotNull(layout);
        assertEquals(3, layout.firstDataLine());
        assertEquals(List.of(2, 1), layout.caseLines());
    }

    @Test
    void readsTheLinesOfCasesWrittenInSeveralColumns(@TempDir Path dir) throws Exception {
        var multiColumn = TableTestProjects.projectModel(dir, "Rules", sheet -> {
            writePolicyTypes(sheet);
            // The same cases, written the other way round: the first case takes two columns.
            TableTestProjects.row(sheet, 11, 1, "Test count acrossTheColumnsTest");
            TableTestProjects.row(sheet, 12, 1, "pol.name", "Policy", "P1", "", "P2");
            TableTestProjects.row(sheet, 13, 1, "pol.vehicles.name", "Vehicle", "A", "B", "C");
            TableTestProjects.row(sheet, 14, 1, "_res_", "Count", "2", "", "1");
            TableTestProjects.merge(sheet, 12, 12, 3, 4);
            TableTestProjects.merge(sheet, 14, 14, 3, 4);
        });

        var layout = TableLayouts.of(multiColumn, TableTestProjects.table(multiColumn, "acrossTheColumnsTest"));

        assertNotNull(layout);
        assertTrue(layout.transposed());
        assertEquals(2, layout.firstDataLine());
        assertEquals(List.of(2, 1), layout.caseLines());
    }

    /** The types and the rule the tables of several lines per case test, from the second row of the sheet. */
    private static void writePolicyTypes(Sheet sheet) {
        TableTestProjects.row(sheet, 1, 1, "Datatype Vehicle");
        TableTestProjects.row(sheet, 2, 1, "String", "name");
        TableTestProjects.row(sheet, 4, 1, "Datatype Policy");
        TableTestProjects.row(sheet, 5, 1, "String", "name");
        TableTestProjects.row(sheet, 6, 1, "Vehicle[]", "vehicles");
        TableTestProjects.row(sheet, 8, 1, "Method Integer count(Policy pol)");
        TableTestProjects.row(sheet, 9, 1, "return length(pol.vehicles);");
    }

    @Test
    void readsATableWrittenTheOtherWayRound() {
        var layout = TableLayouts.of(model, table("acrossTheColumnsTest"));

        assertNotNull(layout);
        assertTrue(layout.transposed(), "a line of this table's data is a column");
        // The field names take the first column and the titles the second; the cases follow.
        assertEquals(2, layout.firstDataLine());
    }

    @Test
    void readsATableThatHoldsASingleCase() {
        var layout = TableLayouts.of(model, table("oneCaseTest"));

        assertNotNull(layout, "the Editor said nothing about a table of one case; this says where it is");
        assertTrue(layout.transposed());
        assertEquals(2, layout.firstDataLine());
    }

    @Test
    void saysNothingAboutATableNobodyAsksThisAbout() {
        assertNull(TableLayouts.of(model, table("greeting")));
    }

    private IOpenLTable table(String name) {
        return model.getAllTableSyntaxNodes()
                .stream()
                .map(node -> model.getTable(node.getUri()))
                .filter(found -> found != null && name.equals(found.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table named " + name));
    }
}
