package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.studio.projects.service.tables.TableTestProjects.projectModel;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;
import static org.openl.studio.projects.service.tables.TableTestProjects.table;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.DeclaredTableEditorsView;
import org.openl.studio.projects.model.tables.TableCellEditorView;
import org.openl.studio.projects.model.tables.TableEditorsView;
import org.openl.types.java.JavaOpenClass;

/**
 * Verifies how a table says its cells are written.
 *
 * <p>The fixture is a decision table whose condition holds whole numbers and whose return holds text, so the
 * numbers are entered within the bounds of their type and the text as any other text; and, beside it, the Data
 * and Test tables whose headers say what a whole line holds — one written the usual way, one the other way
 * round, one standing for the values themselves, and one holding no rows at all.
 */
class TableEditorsReaderTest {

    private ProjectModel module;

    @BeforeEach
    void writeModule(@TempDir Path projectDir) throws Exception {
        module = projectModel(projectDir, "Rules", sheet -> {
            row(sheet, 1, 1, "Rules String Greeting(Hour hour)");
            row(sheet, 2, 1, "C1", "RET1");
            row(sheet, 3, 1, "hour <= max", "value");
            row(sheet, 4, 1, "Integer max", "String value");
            row(sheet, 5, 1, "Max", "Greeting");
            row(sheet, 6, 1, "12", "Good Morning");
            row(sheet, 7, 1, "24", "Good Evening");
            // The header spans both columns, so the matrix holds a cell the merge reaches over.
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 2));

            // The hours a greeting is asked for, and nothing else: a type chosen from a list of its own.
            row(sheet, 9, 1, "Datatype Hour <Integer>");
            row(sheet, 10, 1, "12");
            row(sheet, 11, 1, "24");

            // A table of the values themselves — its one column stands for the row rather than a field of it.
            row(sheet, 13, 1, "Data Integer Hours");
            row(sheet, 14, 1, "this");
            row(sheet, 15, 1, "Hour");
            row(sheet, 16, 1, "12");

            // The same table with its columns declared and nobody having written a row in it yet.
            row(sheet, 18, 1, "Data Integer NoHours");
            row(sheet, 19, 1, "this");
            row(sheet, 20, 1, "Hour");

            row(sheet, 22, 1, "Test Greeting GreetingTest");
            row(sheet, 23, 1, "hour", "_res_");
            row(sheet, 24, 1, "Hour", "Greeting");
            row(sheet, 25, 1, "12", "Good Morning");

            // Written the other way round: every line of it is a row, and the records run across.
            row(sheet, 27, 1, "Test Greeting GreetingTestT");
            row(sheet, 28, 1, "hour", "Hour", "12");
            row(sheet, 29, 1, "_res_", "Greeting", "Good Morning");

            // A table one of whose columns names no field of the type it is a table of.
            row(sheet, 31, 1, "Datatype Policy");
            row(sheet, 32, 1, "String", "name");
            row(sheet, 33, 1, "Integer", "count");
            row(sheet, 35, 1, "Data Policy Policies");
            row(sheet, 36, 1, "name", "", "count");
            row(sheet, 37, 1, "Name", "Note", "Count");
            row(sheet, 38, 1, "Jane", "anything", "3");
        });
    }

    @Test
    void saysHowACellHoldingANumberIsWritten() {
        var read = read("Greeting");

        var numeric = editorOf(read, "numeric");
        assertNotNull(numeric, "a cell holding an Integer is entered as a number");
        assertEquals(Integer.MIN_VALUE, numeric.min());
        assertEquals(Integer.MAX_VALUE, numeric.max());
        assertEquals(Boolean.TRUE, numeric.intOnly());
    }

    @Test
    void keepsAnEditorOnceForEveryCellThatAsksForIt() {
        var read = read("Greeting");

        // Both values of the condition column are entered the same way, and the way is described once — for
        // the column, not for either cell of it.
        assertEquals(1, read.getEditors().stream().filter(editor -> "numeric".equals(editor.editor())).count());
        assertTrue(read.getCells().isEmpty(), "a column says once what every cell of it takes");
        assertEquals(editorOf(read, "numeric"), areaEditor(read, 5, 0));
        assertEquals(editorOf(read, "numeric"), areaEditor(read, 6, 0));
    }

    @Test
    void saysNothingAboutAColumnWrittenAsPlainText() {
        var read = read("Greeting");

        // The return column holds text, which a screen writes as text without being told, so only the
        // condition column is reported at all.
        assertNull(areaEditor(read, 5, 1));
        assertEquals(1, ((DeclaredTableEditorsView) read).getAreas().size());
    }

    @Test
    void saysNothingAboutTheHeadingsTheColumnIsDeclaredIn() {
        var read = read("Greeting");

        // `Integer max` declares what the column holds and `Max` names it; neither is a value of the rules.
        assertNull(areaEditor(read, 3, 0));
        assertNull(areaEditor(read, 4, 0));
    }

    @Test
    void reportsOnlyTheRowsItIsAskedAbout() {
        var window = new TableEditorsReader().read(table(module, "Greeting"), 6, 1);

        // The condition begins above the window and runs on past it, so what is left of it opens the window.
        assertEquals(editorOf(window, "numeric"), areaEditor(window, 0, 0),
                "a part is pointed at by its place in the window, as the raw read reports it");
    }

    @Test
    void readsATableWhoseCellsAMergeReachesOver() {
        var read = read("Greeting");

        // The covered cell of the merged header stands for the cell it belongs to and describes nothing itself;
        // reading it as a cell of its own would fail before any editor is found.
        assertFalse(((DeclaredTableEditorsView) read).getAreas().isEmpty(),
                "the table is read past the merged header");
    }

    @Test
    void saysHowEveryCellOfALineOfADataTableIsWritten() {
        var read = read("Hours");

        // The table's one column stands for the rows themselves, and the table is a table of whole numbers.
        var numeric = editorOf(read, "numeric");
        assertNotNull(numeric, "the column a Data table declares says how its cells are written");
        assertEquals(numeric, areaEditor(read, 3, 0));
        assertNull(areaEditor(read, 2, 0), "the title the column is shown under is no value of it");
    }

    @Test
    void saysHowALineIsWrittenInATableNobodyHasWrittenARowIn() {
        var read = read("NoHours");

        // Nothing stands in the table to be asked about, and a row added to it still holds a whole number.
        var numeric = editorOf(read, "numeric");
        assertNotNull(numeric, "a declared column says how its cells are written before one of them exists");
        assertEquals(numeric, areaEditor(read, 3, 0), "the column holds whole numbers from the row under its title");
    }

    @Test
    void namesACellOnlyWhereItIsWrittenOtherThanItsLine() {
        var read = read("Hours");

        // The one value the table holds is written the way its column is, and the column has said so already.
        assertTrue(read.getCells().isEmpty(), "a line says the same thing about every cell of it, once");
    }

    @Test
    void saysHowTheColumnsOfATestTableAreWritten() {
        var read = read("GreetingTest");

        // The method under test takes an hour, and an hour is one of the two the type lists.
        var combo = areaEditor(read, 3, 0);
        assertNotNull(combo, "the column holding the argument is chosen from what its type allows");
        assertEquals("combo", combo.editor());
        assertEquals(List.of("12", "24"), combo.choices());
        // The result column holds text, which a screen writes as text without being told.
        assertNull(areaEditor(read, 3, 1));
    }

    @Test
    void saysWhichWayRoundATableIsWritten() {
        var read = read("GreetingTestT");

        // Every line of this table is a row of it, and its records run across: the field name and the title
        // stand in the two columns before the values.
        var combo = areaEditor(read, 1, 2);
        assertNotNull(combo, "the row holding the argument is chosen from what its type allows");
        assertEquals(List.of("12", "24"), combo.choices());
        assertNull(areaEditor(read, 1, 1), "the title the row is shown under is no value of it");
    }

    @Test
    void saysWhatTheColumnsOfADecisionTableHold() {
        var read = read("Greeting");

        // A decision table declares its conditions, so it answers for them as a Data table answers for its
        // columns; what it never does is say the same thing about a cell twice.
        assertFalse(((DeclaredTableEditorsView) read).getAreas().isEmpty());
    }

    @Test
    void readsTheColumnsAfterOneThatNamesNoFieldAtAll() {
        var table = table(module, "Policies");
        var region = table.getGridTable().getRegion();
        var metaInfoReader = table.getSyntaxNode().getMetaInfoReader();

        // The middle column names no field, so the table declares nothing about it — but the columns after it
        // are declared as much as the ones before, and each of them holds what its own field was declared with.
        // Walked by a count of the columns declared rather than by the columns themselves, the reading stopped
        // at the one that named nothing and every column past it was left holding no type at all.
        var body = region.getTop() + 3;
        assertEquals(JavaOpenClass.STRING,
                metaInfoReader.getMetaInfo(body, region.getLeft()).getDataType());
        assertNull(metaInfoReader.getMetaInfo(body, region.getLeft() + 1));
        assertEquals(JavaOpenClass.getOpenClass(Integer.class),
                metaInfoReader.getMetaInfo(body, region.getLeft() + 2).getDataType(),
                "the column after the one naming no field holds what it was declared with");
    }

    /** The whole of what the table says about how its cells are written. */
    private TableEditorsView read(String name) {
        return new TableEditorsReader().read(table(module, name), null, null);
    }

    /** The first editor of the given kind, or {@code null} when the table asks for none. */
    private static TableCellEditorView editorOf(TableEditorsView read, String kind) {
        return read.getEditors().stream()
                .filter(editor -> kind.equals(editor.editor()))
                .findFirst()
                .orElse(null);
    }

    /** The editor the cell at the given place asks for, or {@code null} when nothing says how it is written. */
    static TableCellEditorView areaEditor(TableEditorsView read, int row, int column) {
        if (!(read instanceof DeclaredTableEditorsView declared)) {
            return null;
        }
        return declared.getAreas().stream()
                .filter(area -> row >= area.row() && (area.rows() == null || row < area.row() + area.rows()))
                .filter(area -> column >= area.column()
                        && (area.columns() == null || column < area.column() + area.columns()))
                .map(area -> read.getEditors().get(area.editor()))
                .findFirst()
                .orElse(null);
    }
}
