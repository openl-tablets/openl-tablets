package org.openl.studio.compare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls2.XlsDiff2;
import org.openl.studio.compare.model.ComparisonNodeStatus;
import org.openl.studio.compare.model.ComparisonNodeType;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Compares two workbooks written for the test and checks what the API reports about them.
 */
class ComparisonMapperTest {

    private static final String[][] PERSON = {
            {"Datatype Person"},
            {"String", "name"},
            {"int", "age"},
    };

    private static final String[][] PERSON_WITH_ADDRESS = {
            {"Datatype Person"},
            {"String", "name"},
            {"double", "age"},
            {},
            {"Datatype Address"},
            {"String", "city"},
    };

    @TempDir
    Path dir;

    private final ComparisonMapper mapper = new ComparisonMapper(new RawTableReader());

    @Test
    void reportsEveryElementOfTheTwoFilesWithItsStatus() throws IOException {
        var view = mapper.toView("cmp", compare(PERSON, PERSON_WITH_ADDRESS));

        assertEquals("cmp", view.id());
        assertFalse(view.identical());

        var sheets = view.sheets();
        assertEquals(1, sheets.size());
        var sheet = sheets.getFirst();
        assertEquals("Rules", sheet.name());
        assertEquals(ComparisonNodeType.SHEET, sheet.type());
        assertEquals(ComparisonNodeStatus.CHANGED, sheet.status());
        assertEquals("0", sheet.id());

        var tables = sheet.children();
        assertEquals(2, tables.size());
        assertEquals("0-0", tables.get(0).id());
        assertEquals("Datatype Person", tables.get(0).name());
        assertEquals(ComparisonNodeType.TABLE, tables.get(0).type());
        assertEquals(ComparisonNodeStatus.CHANGED, tables.get(0).status());
        assertEquals("0-1", tables.get(1).id());
        assertEquals("Datatype Address", tables.get(1).name());
        assertEquals(ComparisonNodeStatus.ADDED, tables.get(1).status());
    }

    @Test
    void tellsThatTwoFilesOfTheSameContentAreIdentical() throws IOException {
        var view = mapper.toView("cmp", compare(PERSON, PERSON));

        assertTrue(view.identical());
        assertEquals(ComparisonNodeStatus.EQUAL, view.sheets().getFirst().status());
        assertEquals(ComparisonNodeStatus.EQUAL, view.sheets().getFirst().children().getFirst().status());
    }

    @Test
    void readsBothSidesOfAChangedTableAndMarksTheCellsThatDiffer() throws IOException {
        var table = mapper.toTable(compare(PERSON, PERSON_WITH_ADDRESS), "0-0");

        assertNotNull(table);
        assertEquals("Datatype Person", table.name());
        assertEquals(ComparisonNodeStatus.CHANGED, table.status());

        assertNotNull(table.first());
        assertNotNull(table.second());
        assertEquals(3, table.first().source().size());
        assertEquals("int", table.first().source().get(2).getFirst().value());
        assertEquals("double", table.second().source().get(2).getFirst().value());
        assertEquals(List.of("A3"), table.first().changedCells());
        assertEquals(List.of("A3"), table.second().changedCells());
    }

    @Test
    void marksTheCellsOfATableThatChangedBothItsWidthAndItsHeight() throws IOException {
        // One more column of default values, one more field, and the field age renamed to years.
        var grown = new String[][]{
                {"Datatype Person"},
                {"String", "name", "Ann"},
                {"int", "years", "30"},
                {"boolean", "active", "true"},
        };

        var table = mapper.toTable(compare(PERSON, grown), "0-0");

        assertNotNull(table);
        assertEquals(ComparisonNodeStatus.CHANGED, table.status());
        // The columns are paired by what they hold, so the renamed field is marked on both sides.
        assertEquals(List.of("B3"), table.first().changedCells());
        // The added row and the added column are marked as a whole.
        assertEquals(List.of("A4", "B3", "B4", "C1", "C2", "C3", "C4"),
                table.second().changedCells().stream().sorted().toList());
    }

    @Test
    void leavesAMergedHeaderUnmarkedWhenAColumnIsAddedOnItsLeft() throws IOException {
        // The header is merged over every column, so it reaches the added column and the paired ones alike.
        var first = new String[][]{
                {"Datatype Person"},
                {"String", "name"},
                {"int", "age"},
        };
        var second = new String[][]{
                {"Datatype Person"},
                {"note", "String", "name"},
                {"note", "int", "years"},
                {"note", "boolean", "active"},
        };

        var tree = new XlsDiff2().diffFiles(workbook("first.xlsx", first, 2).toFile(),
                workbook("second.xlsx", second, 3).toFile());
        var table = mapper.toTable(tree, "0-0");

        assertNotNull(table);
        assertEquals(List.of("B3"), table.first().changedCells());
        // The added column is marked but for the header, which is compared over the paired columns and kept.
        assertEquals(List.of("A2", "A3", "A4", "B4", "C3", "C4"),
                table.second().changedCells().stream().sorted().toList());
    }

    @Test
    void readsATableOnlyOneFileHoldsWithoutMarkingItsCells() throws IOException {
        var table = mapper.toTable(compare(PERSON, PERSON_WITH_ADDRESS), "0-1");

        assertNotNull(table);
        assertEquals(ComparisonNodeStatus.ADDED, table.status());
        assertNull(table.first());
        assertNotNull(table.second());
        assertEquals("Datatype Address", table.second().source().getFirst().getFirst().value());
        assertTrue(table.second().changedCells().isEmpty());
    }

    @Test
    void reportsThePropertiesOfATableThatReadDifferently() throws IOException {
        var moved = new String[][]{
                {},
                {"Datatype Person"},
                {"String", "name"},
                {"int", "age"},
        };
        var tables = mapper.toView("cmp", compare(PERSON, moved)).sheets().getFirst().children();

        var changes = tables.getFirst().changes();
        assertEquals(1, changes.size());
        assertEquals("location", changes.getFirst().property());
        assertEquals("A1", changes.getFirst().first());
        assertEquals("A2", changes.getFirst().second());
    }

    @Test
    void answersWithNothingForATableTheComparisonDoesNotHold() throws IOException {
        var tree = compare(PERSON, PERSON);

        assertNull(mapper.toTable(tree, "0-9"));
        assertNull(mapper.toTable(tree, "9-0"));
        assertNull(mapper.toTable(tree, "0"));
        assertNull(mapper.toTable(tree, "a-b"));
        // A place with anything left over is not a place, however the leftover is written.
        assertNull(mapper.toTable(tree, "0-0-"));
        assertNull(mapper.toTable(tree, "-0-0"));
    }

    private DiffTreeNode compare(String[][] first, String[][] second) throws IOException {
        return new XlsDiff2().diffFiles(workbook("first.xlsx", first).toFile(),
                workbook("second.xlsx", second).toFile());
    }

    /** Writes the given cells into the sheet "Rules" of a workbook of its own. */
    private Path workbook(String name, String[][] rows) throws IOException {
        return workbook(name, rows, 1);
    }

    /** Writes the given cells into the sheet "Rules" of a workbook of its own, its first row merged over the width. */
    private Path workbook(String name, String[][] rows, int headerWidth) throws IOException {
        var file = dir.resolve(name);
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Rules");
            for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                var row = sheet.createRow(rowIndex);
                for (var columnIndex = 0; columnIndex < rows[rowIndex].length; columnIndex++) {
                    row.createCell(columnIndex).setCellValue(rows[rowIndex][columnIndex]);
                }
            }
            if (headerWidth > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headerWidth - 1));
            }
            workbook.write(out);
        }
        return file;
    }
}
