package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

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
