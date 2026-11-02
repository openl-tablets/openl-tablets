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

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.xls.merge.XlsWorkbookComparator;
import org.openl.rules.xls.merge.compare.WorkbookComparison;
import org.openl.studio.compare.model.ComparisonLineView;
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
        try (var comparison = compare(PERSON, PERSON_WITH_ADDRESS)) {
            var view = mapper.toView("cmp", comparison);

            assertEquals("cmp", view.id());
            assertFalse(view.identical());

            var sheets = view.sheets();
            assertEquals(1, sheets.size());
            var sheet = sheets.getFirst();
            assertEquals("Rules", sheet.name());
            assertEquals(ComparisonNodeType.SHEET, sheet.type());
            assertEquals(ComparisonNodeStatus.CHANGED, sheet.status());
            assertEquals("0", sheet.id());
            assertNull(sheet.firstRange());

            var tables = sheet.children();
            assertEquals(2, tables.size());
            assertEquals("0-0", tables.getFirst().id());
            assertEquals("Datatype Person", tables.getFirst().name());
            assertEquals(ComparisonNodeType.TABLE, tables.getFirst().type());
            assertEquals(ComparisonNodeStatus.CHANGED, tables.getFirst().status());
            assertEquals("A1:B3", tables.getFirst().firstRange());
            assertEquals("A1:B3", tables.getFirst().secondRange());
            assertEquals(Boolean.FALSE, tables.getFirst().moved());
            assertEquals("0-1", tables.get(1).id());
            assertEquals("Datatype Address", tables.get(1).name());
            assertEquals(ComparisonNodeStatus.ADDED, tables.get(1).status());
            assertNull(tables.get(1).firstRange());
            assertEquals("A5:B6", tables.get(1).secondRange());
        }
    }

    @Test
    void tellsThatTwoFilesOfTheSameContentAreIdentical() throws IOException {
        try (var comparison = compare(PERSON, PERSON)) {
            var view = mapper.toView("cmp", comparison);

            assertTrue(view.identical());
            assertEquals(ComparisonNodeStatus.EQUAL, view.sheets().getFirst().status());
            assertEquals(ComparisonNodeStatus.EQUAL, view.sheets().getFirst().children().getFirst().status());
        }
    }

    @Test
    void readsBothSidesOfAChangedTableAndMarksTheCellsThatDiffer() throws IOException {
        try (var comparison = compare(PERSON, PERSON_WITH_ADDRESS)) {
            var table = mapper.toTable(comparison, "0-0");

            assertNotNull(table);
            assertEquals("Datatype Person", table.name());
            assertEquals(ComparisonNodeStatus.CHANGED, table.status());
            assertFalse(table.moved());
            assertFalse(table.formattingChanged());

            assertNotNull(table.first());
            assertNotNull(table.second());
            assertEquals(3, table.first().source().size());
            assertEquals("int", table.first().source().get(2).getFirst().value());
            assertEquals("double", table.second().source().get(2).getFirst().value());
            assertEquals(List.of("A3"), table.first().changedCells());
            assertEquals(List.of("A3"), table.second().changedCells());
            assertEquals(List.of(line(0, 0, ComparisonNodeStatus.EQUAL),
                    line(1, 1, ComparisonNodeStatus.EQUAL),
                    line(2, 2, ComparisonNodeStatus.CHANGED)), table.rows());
            assertEquals(List.of(line(0, 0, ComparisonNodeStatus.CHANGED), line(1, 1, ComparisonNodeStatus.EQUAL)),
                    table.columns());
        }
    }

    @Test
    void linesUpTheRowsAndTheColumnsOfATableThatChangedBothItsWidthAndItsHeight() throws IOException {
        // One more column of default values, one more field, and the field age renamed to years.
        var grown = new String[][]{
                {"Datatype Person"},
                {"String", "name", "Ann"},
                {"int", "years", "30"},
                {"boolean", "active", "true"},
        };

        try (var comparison = compare(PERSON, grown)) {
            var table = mapper.toTable(comparison, "0-0");

            assertNotNull(table);
            assertEquals(ComparisonNodeStatus.CHANGED, table.status());
            // The columns are paired by what they hold, so the renamed field is marked on both sides.
            assertEquals(List.of("B3"), table.first().changedCells());
            assertEquals(List.of("B3"), table.second().changedCells());
            // The added row and the added column are told by the lines, not cell by cell.
            assertEquals(line(null, 3, ComparisonNodeStatus.ADDED), table.rows().getLast());
            assertEquals(List.of(line(0, 0, ComparisonNodeStatus.EQUAL),
                    line(1, 1, ComparisonNodeStatus.CHANGED),
                    line(null, 2, ComparisonNodeStatus.ADDED)), table.columns());
        }
    }

    @Test
    void readsATableOnlyOneFileHoldsWithoutMarkingItsCells() throws IOException {
        try (var comparison = compare(PERSON, PERSON_WITH_ADDRESS)) {
            var table = mapper.toTable(comparison, "0-1");

            assertNotNull(table);
            assertEquals(ComparisonNodeStatus.ADDED, table.status());
            assertNull(table.first());
            assertNotNull(table.second());
            assertEquals("Datatype Address", table.second().source().getFirst().getFirst().value());
            assertTrue(table.second().changedCells().isEmpty());
            assertTrue(table.rows().isEmpty());
            assertTrue(table.columns().isEmpty());
        }
    }

    @Test
    void reportsATableMovedOnItsSheetWithItsTwoPlaces() throws IOException {
        var moved = new String[][]{
                {},
                {},
                {"", "", "", "Datatype Person"},
                {"", "", "", "String", "name"},
                {"", "", "", "int", "age"},
        };
        try (var comparison = compare(PERSON, moved)) {
            var person = mapper.toView("cmp", comparison).sheets().getFirst().children().getFirst();
            assertEquals(ComparisonNodeStatus.MOVED, person.status());
            assertEquals(Boolean.TRUE, person.moved());
            assertEquals("A1:B3", person.firstRange());
            assertEquals("D3:E5", person.secondRange());

            var table = mapper.toTable(comparison, "0-0");
            assertNotNull(table);
            assertTrue(table.moved());
            assertEquals(ComparisonNodeStatus.MOVED, table.status());
            assertEquals("A1:B3", table.firstRange());
            assertEquals("D3:E5", table.secondRange());
            assertTrue(table.second().changedCells().isEmpty());
        }
    }

    @Test
    void answersWithNothingForATableTheComparisonDoesNotHold() throws IOException {
        try (var comparison = compare(PERSON, PERSON)) {
            assertNull(mapper.toTable(comparison, "0-9"));
            assertNull(mapper.toTable(comparison, "9-0"));
            assertNull(mapper.toTable(comparison, "0"));
            assertNull(mapper.toTable(comparison, "a-b"));
            // A place with anything left over is not a place, however the leftover is written.
            assertNull(mapper.toTable(comparison, "0-0-"));
            assertNull(mapper.toTable(comparison, "-0-0"));
        }
    }

    private static ComparisonLineView line(Integer first, Integer second, ComparisonNodeStatus status) {
        return new ComparisonLineView(first, second, status);
    }

    private WorkbookComparison compare(String[][] first, String[][] second) throws IOException {
        return XlsWorkbookComparator.compare(workbook("first.xlsx", first).toFile(),
                workbook("second.xlsx", second).toFile());
    }

    /** Writes the given cells into the sheet "Rules" of a workbook of its own; an empty text leaves the cell out. */
    private Path workbook(String name, String[][] rows) throws IOException {
        var file = dir.resolve(name);
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Rules");
            for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                var row = sheet.createRow(rowIndex);
                for (var columnIndex = 0; columnIndex < rows[rowIndex].length; columnIndex++) {
                    var value = rows[rowIndex][columnIndex];
                    if (!value.isEmpty()) {
                        row.createCell(columnIndex).setCellValue(value);
                    }
                }
            }
            workbook.write(out);
        }
        return file;
    }
}
