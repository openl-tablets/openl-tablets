package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.xls.merge.MergeFixture.RULES;
import static org.openl.rules.xls.merge.MergeFixture.bold;
import static org.openl.rules.xls.merge.MergeFixture.cell;
import static org.openl.rules.xls.merge.MergeFixture.cellOf;
import static org.openl.rules.xls.merge.MergeFixture.deleteRow;
import static org.openl.rules.xls.merge.MergeFixture.insertLine;
import static org.openl.rules.xls.merge.MergeFixture.insertRow;
import static org.openl.rules.xls.merge.MergeFixture.isBold;
import static org.openl.rules.xls.merge.MergeFixture.move;
import static org.openl.rules.xls.merge.MergeFixture.moveTable;
import static org.openl.rules.xls.merge.MergeFixture.number;
import static org.openl.rules.xls.merge.MergeFixture.regionIndex;
import static org.openl.rules.xls.merge.MergeFixture.removeTable;
import static org.openl.rules.xls.merge.MergeFixture.set;
import static org.openl.rules.xls.merge.MergeFixture.tables;
import static org.openl.rules.xls.merge.MergeFixture.text;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.SheetConflictReason;

class XlsTableMergerTest {

    private static final MergeFixture FIXTURE = new MergeFixture(MergeFixture::tables);
    /**
     * The base revision with a styled cell in the data table and a formula on another sheet that reads the rules
     * table.
     */
    private static final MergeFixture MATRIX = new MergeFixture(workbook -> {
        tables(workbook);
        fill(workbook.getSheet(RULES), 11, 2);
        cellOf(workbook.createSheet("Other"), 0, 0).setCellFormula("Rules!B4*2");
    });

    /**
     * The base revision with a sheet whose formulas refer to the rules table and to the data table.
     */
    private static void baseWithFormulas(Workbook workbook) {
        tables(workbook);
        addFormulas(workbook.getSheet(RULES));
    }

    /**
     * Adds a sheet whose formulas refer to the rules table and to the data table.
     */
    private static void addFormulas(Sheet sheet) {
        var other = sheet.getWorkbook().createSheet("Other");
        cellOf(other, 0, 0).setCellFormula("Rules!B4*2");
        cellOf(other, 0, 1).setCellFormula("Rules!B11*2");
    }

    @Test
    void rowInsertedInTheirs_andOtherTableEditedInOurs_areMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 10, 1, 31), sheet -> insertRow(sheet, 4, "UK", 4));
        assertRates(merged, "US", "UK", "CA", "MX");
        assertEquals(31, number(merged, 10, 1));
        assertEquals(List.of(CellRangeAddress.valueOf("A1:B1"),
                CellRangeAddress.valueOf("E1:F1"),
                CellRangeAddress.valueOf("A9:C9")), sorted(merged.getMergedRegions()));
    }

    @Test
    void rowInsertedInOurs_andRowBelowEditedInTheirs_areMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> insertRow(sheet, 4, "UK", 4), sheet -> set(sheet, 5, 1, 30));
        assertRates(merged, "US", "UK", "CA", "MX");
        assertEquals(30, number(merged, 6, 1));
        assertEquals(2, number(merged, 5, 1));
    }

    @Test
    void rowInsertedInTheirs_andRowBelowStyledInOurs_keepsTheStyle() throws IOException {
        // The styled row of OUR revision moves down below the row of THEIR revision.
        var merged = FIXTURE.mergeXlsx(sheet -> fill(sheet, 5, 0), sheet -> insertRow(sheet, 4, "UK", 4));
        assertRates(merged, "US", "UK", "CA", "MX");
        assertEquals(FillPatternType.SOLID_FOREGROUND, cell(merged, 6, 0).getCellStyle().getFillPattern());
        assertEquals(FillPatternType.NO_FILL, cell(merged, 5, 0).getCellStyle().getFillPattern());
    }

    @Test
    void rowsInsertedInBothAtDifferentPlaces_areMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> insertField(sheet, 1, "Double", "price"),
                sheet -> insertField(sheet, 3, "Boolean", "used"));
        assertFields(merged, "Double", "String", "Integer", "Boolean");
    }

    @Test
    void rowsInsertedInBothIntoTheEmptyRowsAboveATable_pushItDown() throws IOException {
        // Each revision grows the rules table into the empty rows above the data table, which would touch it.
        var merged = FIXTURE.mergeXlsx(sheet -> insertRow(sheet, 4, "UK", 4), sheet -> set(sheet, 6, 0, "FR"));
        assertRates(merged, "US", "UK", "CA", "MX", "FR");
        assertDriversPushedDown(merged);
    }

    /**
     * Checks that the data table stands a row lower than the base revision has it, with its merged header.
     */
    private static void assertDriversPushedDown(Sheet merged) {
        assertEquals("Data Driver drivers", text(merged, 9, 0));
        assertEquals(30, number(merged, 11, 1));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A10:C10")));
        assertFalse(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A9:C9")));
    }

    @Test
    void sameRowInsertedInBoth_isMergedOnce() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            insertRow(sheet, 4, "UK", 4);
            set(sheet, 3, 1, 10);
        }, sheet -> insertRow(sheet, 4, "UK", 4));
        assertRates(merged, "US", "UK", "CA", "MX");
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void sameRowInsertedInBothWithOtherFormatting_isMergedOnce() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            insertRow(sheet, 4, "UK", 4);
            bold(sheet, 4, 0);
        }, sheet -> insertRow(sheet, 4, "UK", 4));
        assertRates(merged, "US", "UK", "CA", "MX");
        assertTrue(isBold(merged, 4, 0));
    }

    @Test
    void rowsInsertedAtTheSamePlaceIntoTheEmptyRowsAboveATable_pushItDown() throws IOException {
        // Both rows are kept, so the rules table grows over both empty rows above the data table.
        var merged = FIXTURE.mergeXlsx(sheet -> insertRow(sheet, 4, "UK", 4), sheet -> insertRow(sheet, 4, "DE", 5));
        assertRates(merged, "US", "UK", "DE", "CA", "MX");
        assertDriversPushedDown(merged);
    }

    @Test
    void rowsAppendedInBothAboveATable_areKeptAndPushItDown() throws IOException {
        // Each revision adds a country after the last one, the way OpenL Studio adds a row into the empty rows under it.
        var merged = FIXTURE.mergeXlsx(sheet -> {
            set(sheet, 6, 0, "UK");
            set(sheet, 6, 1, 4);
        }, sheet -> {
            set(sheet, 6, 0, "DE");
            set(sheet, 6, 1, 5);
        });
        assertRates(merged, "US", "CA", "MX", "UK", "DE");
        assertEquals(5, number(merged, 7, 1));
        assertDriversPushedDown(merged);
    }

    @Test
    void columnsAppendedInBothTouchingTheTableBeside_isConflict() throws IOException {
        // Both columns are kept, so the rules table would reach the datatype table beside it, which nothing pushes.
        var sheet = FIXTURE.conflicts(XSSFWorkbook::new,
                rules -> appendCondition(rules, "C2", "state"),
                rules -> appendCondition(rules, "C3", "zip")).sheets().getFirst();
        assertEquals(SheetConflictReason.TOUCHING_TABLES, sheet.reason());
    }

    /**
     * Appends a condition to the rules table after its last column, its merged header stretched over it.
     */
    private static void appendCondition(Sheet sheet, String code, String title) {
        set(sheet, 1, 2, code);
        set(sheet, 2, 2, title);
        for (var row = 3; row <= 5; row++) {
            set(sheet, row, 2, title + row);
        }
        sheet.removeMergedRegion(regionIndex(sheet, "A1:B1"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:C1"));
    }

    @Test
    void rowDeletedInTheirs_andOtherRowEditedInOurs_areMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> deleteRow(sheet, 4));
        assertRates(merged, "US", "MX");
        assertEquals(10, number(merged, 3, 1));
        assertNull(cell(merged, 5, 0));
    }

    @Test
    void rowDeletedInTheirs_andFormattedInOurs_isDeleted() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> bold(sheet, 4, 1), sheet -> deleteRow(sheet, 4));
        assertRates(merged, "US", "MX");
    }

    @Test
    void rowDeletedInTheirs_andEditedInOurs_isConflict() throws IOException {
        FIXTURE.assertConflict(sheet -> set(sheet, 4, 1, 20), sheet -> deleteRow(sheet, 4));
    }

    @Test
    void rowsDeletedInBothNextToEachOther_isMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> deleteRow(sheet, 5), sheet -> deleteRow(sheet, 4));
        assertRates(merged, "US");
    }

    @Test
    void columnInsertedInTheirs_andCellEditedInOurs_areMerged() throws IOException {
        // OpenL Studio stretches the merged header over the inserted column.
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 11, 1, 41), sheet -> {
            move(sheet, "C10:C12", 0, 1);
            sheet.removeMergedRegion(regionIndex(sheet, "A9:C9"));
            sheet.addMergedRegion(CellRangeAddress.valueOf("A9:D9"));
            set(sheet, 9, 2, "city");
            set(sheet, 10, 2, "NY");
            set(sheet, 11, 2, "LA");
        });
        assertEquals(41, number(merged, 11, 1));
        assertEquals("LA", text(merged, 11, 2));
        assertEquals("b", text(merged, 11, 3));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A9:D9")));
    }

    @Test
    void rowInsertedInTheirs_nextToARowWhoseEmptyCellsTheyFormatted_isMerged() throws IOException {
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            // Ann has a name alone, so the formatting of her empty cells must not make her row another one.
            workbook.getSheet(RULES).getRow(10).getCell(1).setBlank();
            workbook.getSheet(RULES).getRow(10).getCell(2).setBlank();
        });
        var merged = fixture.mergeXlsx(sheet -> set(sheet, 10, 0, "Anna"), sheet -> {
            move(sheet, "A12:C12", 1, 0);
            set(sheet, 11, 0, "Cid");
            set(sheet, 11, 1, 50);
            set(sheet, 11, 2, "c");
            bold(sheet, 10, 1);
            bold(sheet, 10, 2);
        });
        assertEquals("Anna", text(merged, 10, 0));
        assertEquals("Cid", text(merged, 11, 0));
        assertEquals("Bob", text(merged, 12, 0));
        assertTrue(isBold(merged, 10, 1));
        assertTrue(isBold(merged, 10, 2));
    }

    @Test
    void rowsAndColumnsInOurs_andFormattingInTheirs_takeOurs() throws IOException {
        var merged = FIXTURE.mergeXlsx(XlsTableMergerTest::insertRowAndColumn, sheet -> bold(sheet, 10, 0));
        assertEquals("NY", text(merged, 10, 2));
        assertEquals("Cid", text(merged, 12, 0));
    }

    @Test
    void formattingInOurs_andRowsAndColumnsInTheirs_takeTheirs() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> bold(sheet, 10, 0), XlsTableMergerTest::insertRowAndColumn);
        assertEquals("NY", text(merged, 10, 2));
        assertEquals("Cid", text(merged, 12, 0));
    }

    /**
     * Inserts a column of cities and a row of a driver into the data table.
     */
    private static void insertRowAndColumn(Sheet sheet) {
        move(sheet, "C10:C12", 0, 1);
        sheet.removeMergedRegion(regionIndex(sheet, "A9:C9"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("A9:D9"));
        set(sheet, 9, 2, "city");
        set(sheet, 10, 2, "NY");
        set(sheet, 11, 2, "LA");
        set(sheet, 12, 0, "Cid");
        set(sheet, 12, 1, 50);
        set(sheet, 12, 2, "SF");
        set(sheet, 12, 3, "c");
    }

    @Test
    void rowsInOursAndColumnsInTheirsOfOneTable_isConflict() throws IOException {
        FIXTURE.assertConflict(sheet -> {
            set(sheet, 12, 0, "Cid");
            set(sheet, 12, 1, 50);
            set(sheet, 12, 2, "c");
        }, sheet -> {
            move(sheet, "C10:C12", 0, 1);
            set(sheet, 9, 2, "city");
        });
    }

    @Test
    void tableMovedInTheirs_andEditedInOurs_isMergedAtTheNewPlace() throws IOException {
        // OpenL Studio moves a table that has no room to grow to the end of the sheet.
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 10, 1, 31), sheet -> {
            moveTable(sheet, "A9:C12", "A9:C9", 6, 0);
            set(sheet, 18, 0, "Cid");
            set(sheet, 18, 1, 50);
            set(sheet, 18, 2, "c");
        });
        assertNull(cell(merged, 8, 0));
        assertNull(cell(merged, 10, 1));
        assertEquals("Data Driver drivers", text(merged, 14, 0));
        assertEquals(31, number(merged, 16, 1));
        assertEquals("Cid", text(merged, 18, 0));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A15:C15")));
        assertFalse(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A9:C9")));
    }

    @Test
    void tableMovedWithValuesUnderItsHeader_isMerged() throws IOException {
        // OpenL Studio writes the header text into every cell of the merged header of a table it moves.
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 10, 1, 31), sheet -> {
            moveTable(sheet, "A9:C12", "A9:C9", 6, 0);
            set(sheet, 14, 1, "Data Driver drivers");
            set(sheet, 14, 2, "Data Driver drivers");
        });
        assertEquals(31, number(merged, 16, 1));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A15:C15")));
    }

    @Test
    void tableMovedInOurs_andEditedInTheirs_isMergedAtOurPlace() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> moveTable(sheet, "A9:C12", "A9:C9", 6, 0),
                sheet -> set(sheet, 10, 1, 31));
        assertNull(cell(merged, 10, 1));
        assertEquals(31, number(merged, 16, 1));
    }

    @Test
    void tableMovedToDifferentPlaces_isConflict() throws IOException {
        FIXTURE.assertConflict(sheet -> moveTable(sheet, "A9:C12", "A9:C9", 6, 0),
                sheet -> moveTable(sheet, "A9:C12", "A9:C9", 0, 8));
    }

    @Test
    void tableRemovedInTheirs_andFormattedInOurs_isRemoved() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            bold(sheet, 10, 1);
            set(sheet, 3, 1, 10);
        }, sheet -> removeTable(sheet, "A9:C12", "A9:C9"));
        assertFalse(XlsSheetsMatcher.hasValue(cell(merged, 8, 0)));
        assertFalse(XlsSheetsMatcher.hasValue(cell(merged, 10, 1)));
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void sameTableAddedInBothWithOtherFormatting_isMergedOnce() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            set(sheet, 0, 7, "Data X x");
            bold(sheet, 0, 7);
        }, sheet -> set(sheet, 0, 7, "Data X x"));
        assertEquals("Data X x", text(merged, 0, 7));
        assertTrue(isBold(merged, 0, 7));
    }

    @Test
    void sameTableAddedInBothAtDifferentPlaces_isKeptOnceWhereOursHasIt() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            set(sheet, 14, 0, "Data X x");
            set(sheet, 15, 0, "x1");
        }, sheet -> {
            set(sheet, 0, 8, "Data X x");
            set(sheet, 1, 8, "x1");
        });
        assertEquals("Data X x", text(merged, 14, 0));
        assertEquals("x1", text(merged, 15, 0));
        assertFalse(XlsSheetsMatcher.hasValue(cell(merged, 0, 8)));
        assertFalse(XlsSheetsMatcher.hasValue(cell(merged, 1, 8)));
    }

    @Test
    void tableRemovedInTheirs_andEditedInOurs_isConflict() throws IOException {
        FIXTURE.assertConflict(sheet -> set(sheet, 10, 1, 31), sheet -> removeTable(sheet, "A9:C12", "A9:C9"));
    }

    @Test
    void sheetRowInsertedInTheirs_movesTheTablesBelow() throws IOException {
        // Excel inserts a row through the whole sheet, so the data table below moves with it.
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 10, 1, 31), sheet -> {
            sheet.shiftRows(4, sheet.getLastRowNum(), 1);
            set(sheet, 4, 0, "UK");
            set(sheet, 4, 1, 4);
        });
        assertRates(merged, "US", "UK", "CA", "MX");
        assertEquals("Data Driver drivers", text(merged, 9, 0));
        assertEquals(31, number(merged, 11, 1));
        assertEquals(40, number(merged, 12, 1));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A10:C10")));
    }

    @Test
    void mergedCellsOfOursCrossedByRowOfTheirs_isConflict() throws IOException {
        FIXTURE.assertConflict(sheet -> sheet.addMergedRegion(CellRangeAddress.valueOf("B5:B6")),
                sheet -> insertRow(sheet, 5, "UK", 4));
    }

    @Test
    void mergedCellsAddedInTheirsOverInsertedRow_comeWithTheRow() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> insertField(sheet, 1, "Double", "price"), sheet -> {
            insertField(sheet, 2, "Comment", "");
            sheet.addMergedRegion(CellRangeAddress.valueOf("E3:F3"));
        });
        assertFields(merged, "Double", "String", "Comment", "Integer");
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("E4:F4")));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("E1:F1")));
    }

    @Test
    void tableCreatedInTheirs_andRowInsertedInOurs_areMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> insertRow(sheet, 4, "UK", 4), sheet -> {
            set(sheet, 14, 0, "Data Car cars");
            set(sheet, 15, 0, "model");
            set(sheet, 16, 0, "Volvo");
        });
        assertRates(merged, "US", "UK", "CA", "MX");
        assertEquals("Volvo", text(merged, 16, 0));
    }

    @Test
    void formulaReferringToMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, XlsTableMergerTest::addFormulas);
    }

    @Test
    void formulaReferringToTableInPlace_isRecalculated() throws IOException {
        var fixture = new MergeFixture(XlsTableMergerTest::baseWithFormulas);
        var merged = fixture.mergeXlsx(
                sheet -> set(sheet, 3, 1, 10),
                sheet -> insertRow(sheet, 5, "UK", 4));
        assertRates(merged, "US", "CA", "UK", "MX");
        assertEquals(20, merged.getWorkbook().getSheet("Other").getRow(0).getCell(0).getNumericCellValue());
    }

    @Test
    void xlsFormulaReferringToMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(HSSFWorkbook::new, XlsTableMergerTest::addFormulas);
    }

    @Test
    void definedNameOverMovedRows_isConflict() throws IOException {
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            var name = workbook.createName();
            name.setNameName("Rates");
            name.setRefersToFormula("Rules!$B$5:$B$6");
        });
        var diff = fixture.diff(XSSFWorkbook::new,
                sheet -> set(sheet, 10, 1, 31),
                sheet -> insertRow(sheet, 4, "UK", 4));
        assertTrue(diff.hasConflicts());
    }

    @Test
    void dataValidationOverMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> {
            var helper = sheet.getDataValidationHelper();
            var constraint = helper.createExplicitListConstraint(new String[]{"a", "b"});
            sheet.addValidationData(helper.createValidation(constraint, new CellRangeAddressList(10, 11, 2, 2)));
        });
    }

    @Test
    void rowsInsertedAtTwoPlacesInTheirs_andRowEditedInOurs_areMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 1, 5, "brand"), sheet -> {
            insertField(sheet, 1, "Double", "price");
            insertField(sheet, 3, "Boolean", "used");
        });
        assertFields(merged, "Double", "String", "Boolean", "Integer");
        assertEquals("price", text(merged, 1, 5));
        assertEquals("brand", text(merged, 2, 5));
        assertEquals("used", text(merged, 3, 5));
        assertEquals("year", text(merged, 4, 5));
    }

    @Test
    void formulaOfTheirsReferringToTableMovedInOurs_isConflict() throws IOException {
        // The new sheet of THEIR revision is taken whole, and its formula still points at the old place of the table.
        var diff = FIXTURE.diff(XSSFWorkbook::new, sheet -> {
            sheet.shiftRows(4, sheet.getLastRowNum(), 1);
            set(sheet, 4, 0, "UK");
            set(sheet, 4, 1, 4);
        }, sheet -> {
            set(sheet, 3, 1, 10);
            cellOf(sheet.getWorkbook().createSheet("Report"), 0, 0).setCellFormula("Rules!B11*2");
        });
        assertTrue(diff.hasConflicts());
    }

    @Test
    void formulaOfTheirsReferringToMovedTableThroughName_isConflict() throws IOException {
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            var name = workbook.createName();
            name.setNameName("Ages");
            name.setRefersToFormula("Rules!$B$11:$B$12");
        });
        var diff = fixture.diff(XSSFWorkbook::new, sheet -> {
            sheet.shiftRows(4, sheet.getLastRowNum(), 1);
            set(sheet, 4, 0, "UK");
            set(sheet, 4, 1, 4);
        }, sheet -> {
            set(sheet, 3, 1, 10);
            cellOf(sheet.getWorkbook().createSheet("Report"), 0, 0).setCellFormula("SUM(Ages)");
        });
        assertTrue(diff.hasConflicts());
    }

    @Test
    void formulaOfTheirsReferringToMovedTableThroughChainedNames_isConflict() throws IOException {
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            // the name referring to another name comes first, so it is checked first
            var total = workbook.createName();
            total.setNameName("Total");
            total.setRefersToFormula("0");
            var ages = workbook.createName();
            ages.setNameName("Ages");
            ages.setRefersToFormula("Rules!$B$11:$B$12");
            total.setRefersToFormula("SUM(Ages)");
        });
        var diff = fixture.diff(XSSFWorkbook::new, sheet -> {
            sheet.shiftRows(4, sheet.getLastRowNum(), 1);
            set(sheet, 4, 0, "UK");
            set(sheet, 4, 1, 4);
        }, sheet -> {
            set(sheet, 3, 1, 10);
            cellOf(sheet.getWorkbook().createSheet("Report"), 0, 0).setCellFormula("Total");
        });
        assertTrue(diff.hasConflicts());
    }

    @Test
    void rowHiddenInTheirsWhereTheirRowsMove_isShown() throws IOException {
        // A hidden row is kept by its index, so it would hide the row of OUR revision that moves onto it.
        var merged = FIXTURE.mergeXlsx(sheet -> insertRow(sheet, 4, "UK", 4), sheet -> {
            set(sheet, 3, 1, 10);
            sheet.getRow(5).setZeroHeight(true);
        });
        assertEquals(10, number(merged, 3, 1));
        assertEquals("UK", text(merged, 4, 0));
        assertEquals("MX", text(merged, 6, 0));
        assertFalse(merged.getRow(5).getZeroHeight());
        assertFalse(merged.getRow(6).getZeroHeight());
    }

    @Test
    void commentOfMovedCell_staysNextToIt() throws IOException {
        var merged = FIXTURE.merge(HSSFWorkbook::new,
                sheet -> MergeFixture.comment(sheet, 4, 0, "C5:D7", "checked"),
                sheet -> insertRow(sheet, 4, "UK", 4));
        var comment = cell(merged, 5, 0).getCellComment();
        assertEquals("checked", comment.getString().getString());
        assertEquals(5, comment.getClientAnchor().getRow1());
        assertNull(cell(merged, 4, 0).getCellComment());
    }

    @Test
    void pictureOverMovedTable_isConflict() throws IOException {
        // A picture keeps its cells, so it would stay behind the table it shows.
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> MergeFixture.addPicture(sheet, "B10:C11"));
    }

    @Test
    void pictureAwayFromMovedTable_isMerged() throws IOException {
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            MergeFixture.addPicture(workbook.getSheet(RULES), "H1:I3");
        });
        var merged = fixture.mergeXlsx(
                sheet -> set(sheet, 10, 1, 31),
                sheet -> moveTable(sheet, "A9:C12", "A9:C9", 6, 0));
        assertEquals(31, number(merged, 16, 1));
    }

    @Test
    void dataValidationListFromMovedTable_isConflict() throws IOException {
        // The list lies away from the table, but its values come from cells that move.
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> {
            var helper = sheet.getDataValidationHelper();
            var constraint = helper.createFormulaListConstraint("$B$11:$B$12");
            sheet.addValidationData(helper.createValidation(constraint, new CellRangeAddressList(0, 0, 7, 7)));
        });
    }

    @Test
    void conditionalFormattingConditionOnMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> {
            var formatting = sheet.getSheetConditionalFormatting();
            formatting.addConditionalFormatting(new CellRangeAddress[]{CellRangeAddress.valueOf("H1")},
                    formatting.createConditionalFormattingRule("$B$11>35"));
        });
    }

    @Test
    void conditionalFormattingConditionReachingMovedTableFromItsSecondRange_isConflict() throws IOException {
        // B1 is read at H1; at I10, nine rows down and a column right, the same condition reads C10.
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> {
            var formatting = sheet.getSheetConditionalFormatting();
            formatting.addConditionalFormatting(new CellRangeAddress[]{
                    CellRangeAddress.valueOf("H1"),
                    CellRangeAddress.valueOf("I10")}, formatting.createConditionalFormattingRule("B1>0"));
        });
    }

    @Test
    void xlsFormulaOverRangeOfSheetsEndingAtMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(HSSFWorkbook::new, sheet -> {
            var workbook = sheet.getWorkbook();
            workbook.createSheet("First");
            workbook.setSheetOrder("First", 0);
            cellOf(workbook.createSheet("Other"), 0, 0).setCellFormula("SUM(First:Rules!B11)");
        });
    }

    @Test
    void headerChangedInOurs_andRowInsertedInTheirs_areMerged() throws IOException {
        // The table is found at its place although its header changed.
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 8, 0, "Data Driver people"), sheet -> {
            move(sheet, "A11:C12", 1, 0);
            set(sheet, 10, 0, "Cid");
            set(sheet, 10, 1, 50);
            set(sheet, 10, 2, "c");
        });
        assertEquals("Data Driver people", text(merged, 8, 0));
        assertEquals("Cid", text(merged, 10, 0));
        assertEquals("Ann", text(merged, 11, 0));
        assertEquals("Bob", text(merged, 12, 0));
    }

    @Test
    void sameCellChangedDifferentlyAfterRowInserted_isConflict() throws IOException {
        FIXTURE.assertConflict(sheet -> {
            insertRow(sheet, 4, "UK", 4);
            set(sheet, 5, 1, 20);
        }, sheet -> set(sheet, 4, 1, 30));
    }

    @Test
    void valueOfMovedRowInOurs_andItsStyleInTheirs_areMerged() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            insertRow(sheet, 4, "UK", 4);
            set(sheet, 6, 1, 30);
        }, sheet -> fill(sheet, 5, 1));
        assertEquals(30, number(merged, 6, 1));
        assertEquals(FillPatternType.SOLID_FOREGROUND, cell(merged, 6, 1).getCellStyle().getFillPattern());
    }

    @Test
    void differentColumnsInsertedAtTheSamePlace_areMergedOursFirst() throws IOException {
        // The merged header covers the inserted column in each revision, and the column of both in the merged one.
        var merged = FIXTURE.mergeXlsx(sheet -> {
            move(sheet, "C10:C12", 0, 1);
            set(sheet, 9, 2, "zip");
        }, sheet -> {
            move(sheet, "C10:C12", 0, 1);
            set(sheet, 9, 2, "city");
        });
        assertEquals("zip", text(merged, 9, 2));
        assertEquals("city", text(merged, 9, 3));
        assertEquals("note", text(merged, 9, 4));
        assertEquals("b", text(merged, 11, 4));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A9:D9")));
    }

    @Test
    void shapeOverMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> {
            var anchor = sheet.getWorkbook().getCreationHelper().createClientAnchor();
            anchor.setRow1(9);
            anchor.setCol1(1);
            anchor.setRow2(10);
            anchor.setCol2(2);
            ((XSSFDrawing) sheet.createDrawingPatriarch()).createSimpleShape((XSSFClientAnchor) anchor);
        });
    }

    @Test
    void columnWidthChangedInTheirsWhereOurRowsMove_isMerged() throws IOException {
        // THEIR revision both inserts the row and widens the column, so its own content stays under the width.
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            insertRow(sheet, 4, "UK", 4);
            sheet.setColumnWidth(1, 9000);
        });
        assertEquals(9000, merged.getColumnWidth(1));
        assertEquals(10, number(merged, 3, 1));
        assertEquals("UK", text(merged, 4, 0));
        assertEquals("CA", text(merged, 5, 0));
    }

    @Test
    void rowHeightSetInTheirsWhereTheirRowMovedTo_isMerged() throws IOException {
        // THEIR revision laid out the row MX moves up to, as THEIR revision has it.
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            deleteRow(sheet, 4);
            sheet.getRow(4).setHeight((short) 600);
        });
        assertEquals(10, number(merged, 3, 1));
        assertEquals("MX", text(merged, 4, 0));
        assertEquals(600, merged.getRow(4).getHeight());
    }

    @Test
    void rowHeightSetInOursWhereOurRowsMove_staysAtItsRow() throws IOException {
        // OUR revision laid out the row of MX, which moves up when THEIR revision deletes CA.
        var merged = FIXTURE.mergeXlsx(sheet -> {
            set(sheet, 3, 1, 10);
            sheet.getRow(5).setHeight((short) 600);
        }, sheet -> deleteRow(sheet, 4));
        assertEquals(10, number(merged, 3, 1));
        assertEquals("MX", text(merged, 4, 0));
        assertEquals(600, merged.getRow(5).getHeight());
    }

    @Test
    void rowInsertedNextToRowDeletedInTheirs_isConflict() throws IOException {
        // The new row of OUR revision was meant to stand before CA, which THEIR revision deletes.
        FIXTURE.assertConflict(sheet -> insertRow(sheet, 4, "UK", 4), sheet -> deleteRow(sheet, 4));
    }

    @Test
    void hyperlinkToMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> {
            var link = sheet.getWorkbook().getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
            link.setAddress("Rules!B11");
            cellOf(sheet, 0, 7).setCellValue("Ann");
            sheet.getRow(0).getCell(7).setHyperlink(link);
        });
    }

    @Test
    void excelTableOverMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> ((XSSFSheet) sheet).createTable(
                new AreaReference("A10:C12", SpreadsheetVersion.EXCEL2007)));
    }

    @Test
    void autofilterOverMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new,
                sheet -> sheet.setAutoFilter(CellRangeAddress.valueOf("A10:C12")));
    }

    @Test
    void xlsPictureOverMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(HSSFWorkbook::new, sheet -> MergeFixture.addPicture(sheet, "B10:C11"));
    }

    /**
     * Checks that the sheet stays a conflict when THEIR revision moves the data table and the base revision has
     * something more on the sheet.
     */
    private static void assertConflictWhenDataTableMoves(Supplier<Workbook> format, Consumer<Sheet> extra)
            throws IOException {
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            extra.accept(workbook.getSheet(RULES));
        });
        var diff = fixture.diff(format,
                sheet -> set(sheet, 3, 1, 10),
                sheet -> moveTable(sheet, "A9:C12", "A9:C9", 6, 0));
        assertTrue(diff.hasConflicts());
    }

    @Test
    void xlsCommentOfTableMovedToTheTop_staysOnTheSheet() throws IOException {
        // The comment box stands a row above its cell; the table moves to the first row of the sheet.
        var fixture = new MergeFixture(workbook -> {
            var sheet = workbook.createSheet(RULES);
            set(sheet, 8, 0, "Data Driver drivers");
            set(sheet, 9, 0, "name");
            set(sheet, 10, 0, "Ann");
            MergeFixture.comment(sheet, 8, 0, "B8:D10", "checked");
        });
        var merged = fixture.merge(HSSFWorkbook::new, sheet -> set(sheet, 10, 0, "Anna"), sheet -> {
            sheet.getRow(8).getCell(0).removeCellComment();
            move(sheet, "A9:A11", -8, 0);
            MergeFixture.comment(sheet, 0, 0, "B1:D3", "checked");
        });
        assertEquals("Anna", text(merged, 2, 0));
        var comment = cell(merged, 0, 0).getCellComment();
        assertEquals("checked", comment.getString().getString());
        assertEquals(0, comment.getClientAnchor().getRow1());
    }

    @Test
    void conditionalFormattingOverMovedTable_isConflict() throws IOException {
        assertConflictWhenDataTableMoves(XSSFWorkbook::new, sheet -> {
            var formatting = sheet.getSheetConditionalFormatting();
            formatting.addConditionalFormatting(new CellRangeAddress[]{CellRangeAddress.valueOf("B11:B12")},
                    formatting.createConditionalFormattingRule("B11>35"));
        });
    }

    @Test
    void tablesGrownTowardsEachOther_isConflict() throws IOException {
        // The rules table grows into the empty row above the data table, and the data table grows into it too.
        FIXTURE.assertConflict(sheet -> {
            set(sheet, 6, 0, "FR");
            set(sheet, 6, 1, 5);
        }, sheet -> moveTable(sheet, "A9:C12", "A9:C9", -1, 0));
    }

    @Test
    void xlsWorkbook_isMergedByRows() throws IOException {
        var merged = FIXTURE.merge(HSSFWorkbook::new,
                sheet -> set(sheet, 10, 1, 31),
                sheet -> insertRow(sheet, 4, "UK", 4));
        assertRates(merged, "US", "UK", "CA", "MX");
        assertEquals(31, number(merged, 10, 1));
    }

    @Test
    void sheetMergedByTables_isListedAsMerged() throws IOException {
        var diff = FIXTURE.diff(XSSFWorkbook::new,
                sheet -> set(sheet, 10, 1, 31),
                sheet -> insertRow(sheet, 4, "UK", 4));
        assertEquals(List.of(RULES), List.copyOf(diff.getSheetDiffResult().getDiffSheets(DiffStatus.MERGED)));
    }

    /**
     * What a revision does with the data table, besides changing the rules table.
     */
    enum Action {
        EQUAL,
        CREATED,
        REMOVED,
        CHANGED,
        MOVED
    }

    static Stream<Arguments> matrix() {
        return Arrays.stream(Action.values())
                .flatMap(our -> Arrays.stream(Action.values()).map(their -> Arguments.of(our, their)));
    }

    @ParameterizedTest(name = "OUR {0}, THEIR {1}")
    @MethodSource("matrix")
    void dataTable_isMergedByWhatEachRevisionDid(Action our, Action their) throws IOException {
        Consumer<Sheet> ourEdit = sheet -> {
            set(sheet, 3, 1, 10);
            act(sheet, our, 10);
        };
        Consumer<Sheet> theirEdit = sheet -> {
            set(sheet, 5, 1, 30);
            act(sheet, their, 11);
        };
        var removedAndChanged = our == Action.REMOVED && their == Action.CHANGED
                || our == Action.CHANGED && their == Action.REMOVED;
        if (removedAndChanged) {
            MATRIX.assertConflict(ourEdit, theirEdit);
            return;
        }
        var merged = MATRIX.mergeXlsx(ourEdit, theirEdit);
        assertEquals(10, number(merged, 3, 1));
        assertEquals(30, number(merged, 5, 1));
        assertEquals(20, merged.getWorkbook().getSheet("Other").getRow(0).getCell(0).getNumericCellValue());
        var removed = our == Action.REMOVED || their == Action.REMOVED;
        var top = our == Action.MOVED || their == Action.MOVED ? 20 : 8;
        if (removed) {
            assertNull(cell(merged, 8, 0));
            assertNull(cell(merged, 20, 0));
        } else {
            assertNull(cell(merged, top == 8 ? 20 : 8, 0));
            assertEquals("Data Driver drivers", text(merged, top, 0));
            assertEquals(our == Action.CHANGED ? 31 : 30, number(merged, top + 2, 1));
            assertEquals(their == Action.CHANGED ? 41 : 40, number(merged, top + 3, 1));
            assertTrue(merged.getMergedRegions()
                    .contains(new CellRangeAddress(top, top, 0, 2)));
            assertEquals(FillPatternType.SOLID_FOREGROUND, cell(merged, top + 3, 2).getCellStyle().getFillPattern());
        }
        var created = our == Action.CREATED || their == Action.CREATED;
        assertEquals(created ? "Data Car cars" : null,
                cell(merged, 14, 4) == null ? null : text(merged, 14, 4));
    }

    private static void act(Sheet sheet, Action action, int changedRow) {
        switch (action) {
            case EQUAL -> {
                // the data table stays as it is
            }
            case CREATED -> {
                set(sheet, 14, 4, "Data Car cars");
                set(sheet, 15, 4, "model");
                set(sheet, 16, 4, "Volvo");
            }
            case REMOVED -> removeTable(sheet, "A9:C12", "A9:C9");
            case CHANGED -> set(sheet, changedRow, 1, number(sheet, changedRow, 1) + 1);
            case MOVED -> moveTable(sheet, "A9:C12", "A9:C9", 12, 0);
        }
    }

    /**
     * Checks the countries of the rules table from its first row of values, and that the table ends after them.
     */
    private static void assertRates(Sheet sheet, String... countries) {
        for (var i = 0; i < countries.length; i++) {
            assertEquals(countries[i], text(sheet, 3 + i, 0));
        }
        var after = cell(sheet, 3 + countries.length, 0);
        assertTrue(after == null || after.getCellType() == CellType.BLANK);
    }

    /**
     * Checks the types of the fields of the datatype table, and that the table ends after them.
     */
    private static void assertFields(Sheet sheet, String... types) {
        for (var i = 0; i < types.length; i++) {
            assertEquals(types[i], text(sheet, 1 + i, 4));
        }
        assertNull(cell(sheet, 1 + types.length, 4));
    }

    /**
     * Inserts a field into the datatype table: the fields below it move down.
     */
    private static void insertField(Sheet sheet, int row, String type, String name) {
        insertLine(sheet, row, 4);
        set(sheet, row, 4, type);
        if (!name.isEmpty()) {
            set(sheet, row, 5, name);
        }
    }

    private static void fill(Sheet sheet, int row, int column) {
        var style = sheet.getWorkbook().createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellOf(sheet, row, column).setCellStyle(style);
    }

    private static List<CellRangeAddress> sorted(List<CellRangeAddress> regions) {
        return regions.stream()
                .sorted(Comparator.comparingInt(CellRangeAddress::getFirstRow)
                        .thenComparingInt(CellRangeAddress::getFirstColumn))
                .toList();
    }
}
