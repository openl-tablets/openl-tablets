package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.xls.merge.MergeFixture.RULES;
import static org.openl.rules.xls.merge.MergeFixture.bold;
import static org.openl.rules.xls.merge.MergeFixture.cell;
import static org.openl.rules.xls.merge.MergeFixture.cellOf;
import static org.openl.rules.xls.merge.MergeFixture.deleteRow;
import static org.openl.rules.xls.merge.MergeFixture.insertRow;
import static org.openl.rules.xls.merge.MergeFixture.isBold;
import static org.openl.rules.xls.merge.MergeFixture.layOut;
import static org.openl.rules.xls.merge.MergeFixture.moveTable;
import static org.openl.rules.xls.merge.MergeFixture.number;
import static org.openl.rules.xls.merge.MergeFixture.removeTable;
import static org.openl.rules.xls.merge.MergeFixture.set;
import static org.openl.rules.xls.merge.MergeFixture.tables;
import static org.openl.rules.xls.merge.MergeFixture.text;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.SheetConflictReason;
import org.openl.rules.xls.merge.diff.SheetConflicts;
import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.TableConflictReason;
import org.openl.rules.xls.merge.diff.UnresolvedConflictsException;
import org.openl.rules.xls.merge.diff.WorkbookConflicts;

class XlsConflictResolutionTest {

    private static final String RATES = "Rules/A1:B6";
    private static final String DRIVERS = "Rules/A9:C12";
    private static final MergeFixture FIXTURE = new MergeFixture(MergeFixture::tables);
    /**
     * The tables with another sheet, so a workbook keeps a sheet when the sheet of the tables is removed.
     */
    private static final MergeFixture TWO_SHEETS = new MergeFixture(workbook -> {
        tables(workbook);
        set(workbook.createSheet("Other"), 0, 0, "kept");
    });
    /**
     * The rules table and the datatype table, without the data table below them.
     */
    private static final MergeFixture NO_DATA = new MergeFixture(workbook -> {
        tables(workbook);
        removeTable(workbook.getSheet(RULES), "A9:C12", "A9:C9");
    });

    /**
     * Both revisions change the rate of US, OUR revision a field of the datatype, THEIR revision the age of Ann.
     */
    private static final Consumer<Sheet> OUR_RATE = sheet -> {
        set(sheet, 3, 1, 10);
        set(sheet, 1, 5, "brand");
    };
    private static final Consumer<Sheet> THEIR_RATE = sheet -> {
        set(sheet, 3, 1, 20);
        set(sheet, 10, 1, 31);
    };

    @Test
    void sameCellChangedInBoth_isTheConflictOfItsTable() throws IOException {
        var conflicts = FIXTURE.conflicts(XSSFWorkbook::new, OUR_RATE, THEIR_RATE);

        var range = CellRangeAddress.valueOf("A1:B6");
        assertEquals(new WorkbookConflicts(List.of(new SheetConflicts(RULES,
                DiffStatus.CONFLICT,
                null,
                true,
                true,
                true,
                List.of(new TableConflict(RATES, range, range, range, TableConflictReason.CELLS)))), false), conflicts);
    }

    @Test
    void tableTakenFromTheirs_keepsTheOtherChangesOfBoth() throws Exception {
        var merged = FIXTURE.resolveXlsx(OUR_RATE, THEIR_RATE, Map.of(RATES, Resolution.THEIRS)).getSheet(RULES);

        assertEquals(20, number(merged, 3, 1));
        assertEquals("brand", text(merged, 1, 5));
        assertEquals(31, number(merged, 10, 1));
    }

    @Test
    void tableTakenFromOurs_keepsTheOtherChangesOfBoth() throws Exception {
        var merged = FIXTURE.resolveXlsx(OUR_RATE, THEIR_RATE, Map.of(RATES, Resolution.OURS)).getSheet(RULES);

        assertEquals(10, number(merged, 3, 1));
        assertEquals("brand", text(merged, 1, 5));
        assertEquals(31, number(merged, 10, 1));
    }

    @Test
    void tableTakenFromBase_dropsTheChangesOfBothToIt() throws Exception {
        var merged = FIXTURE.resolveXlsx(OUR_RATE, THEIR_RATE, Map.of(RATES, Resolution.BASE)).getSheet(RULES);

        assertEquals(1, number(merged, 3, 1));
        assertEquals("brand", text(merged, 1, 5));
        assertEquals(31, number(merged, 10, 1));
        assertEquals(List.of(CellRangeAddress.valueOf("A1:B1"),
                CellRangeAddress.valueOf("E1:F1"),
                CellRangeAddress.valueOf("A9:C9")), sorted(merged));
    }

    @Test
    void xlsTableTakenFromBase_keepsItsStyles() throws Exception {
        var merged = FIXTURE.resolve(HSSFWorkbook::new, OUR_RATE, sheet -> {
            THEIR_RATE.accept(sheet);
            var bold = sheet.getWorkbook().createFont();
            bold.setBold(true);
            var style = sheet.getWorkbook().createCellStyle();
            style.setFont(bold);
            cell(sheet, 3, 0).setCellStyle(style);
        }, Map.of(RATES, Resolution.BASE)).getSheet(RULES);

        assertEquals(1, number(merged, 3, 1));
        assertFalse(merged.getWorkbook().getFontAt(cell(merged, 3, 0).getCellStyle().getFontIndex()).getBold());
        assertEquals(31, number(merged, 10, 1));
    }

    @Test
    void rowsInsertedAtTheSamePlace_areTakenWithTheTableOfOneRevision() throws Exception {
        Consumer<Sheet> our = sheet -> insertRow(sheet, 4, "UK", 4);
        Consumer<Sheet> their = sheet -> insertRow(sheet, 4, "DE", 5);

        var table = FIXTURE.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst().tables().getFirst();
        assertEquals(new TableConflict(RATES,
                CellRangeAddress.valueOf("A1:B7"),
                CellRangeAddress.valueOf("A1:B6"),
                CellRangeAddress.valueOf("A1:B7"),
                TableConflictReason.ROWS), table);

        assertCountries(FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.THEIRS)), "US", "DE", "CA", "MX");
        assertCountries(FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.OURS)), "US", "UK", "CA", "MX");
        assertCountries(FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.BASE)), "US", "CA", "MX");
    }

    @Test
    void rowDeletedInTheirs_andChangedInOurs_isTakenWithTheTableOfOneRevision() throws Exception {
        Consumer<Sheet> our = sheet -> set(sheet, 4, 1, 20);
        Consumer<Sheet> their = sheet -> deleteRow(sheet, 4);

        assertEquals(TableConflictReason.ROWS,
                FIXTURE.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst().tables().getFirst().reason());
        assertCountries(FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.THEIRS)), "US", "MX");
        var merged = FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.OURS));
        assertCountries(merged, "US", "CA", "MX");
        assertEquals(20, number(merged.getSheet(RULES), 4, 1));
    }

    @Test
    void tableMovedToDifferentPlaces_isTakenAtThePlaceOfTheRevisionChosen() throws Exception {
        Consumer<Sheet> our = sheet -> moveTable(sheet, "A9:C12", "A9:C9", 6, 0);
        Consumer<Sheet> their = sheet -> moveTable(sheet, "A9:C12", "A9:C9", 0, 4);

        assertEquals(new TableConflict(DRIVERS,
                CellRangeAddress.valueOf("A15:C18"),
                CellRangeAddress.valueOf("A9:C12"),
                CellRangeAddress.valueOf("E9:G12"),
                TableConflictReason.MOVED), FIXTURE.conflicts(XSSFWorkbook::new, our, their)
                .sheets()
                .getFirst()
                .tables()
                .getFirst());

        var merged = FIXTURE.resolveXlsx(our, their, Map.of(DRIVERS, Resolution.THEIRS)).getSheet(RULES);
        assertEquals("Data Driver drivers", text(merged, 8, 4));
        assertNull(cell(merged, 14, 0));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("E9:G9")));
    }

    @Test
    void tableRemovedInTheirs_andChangedInOurs_isRemovedOrKept() throws Exception {
        Consumer<Sheet> our = sheet -> set(sheet, 10, 1, 31);
        Consumer<Sheet> their = sheet -> removeTable(sheet, "A9:C12", "A9:C9");

        assertEquals(new TableConflict(DRIVERS,
                CellRangeAddress.valueOf("A9:C12"),
                CellRangeAddress.valueOf("A9:C12"),
                null,
                TableConflictReason.REMOVED_AND_CHANGED), FIXTURE.conflicts(XSSFWorkbook::new, our, their)
                .sheets()
                .getFirst()
                .tables()
                .getFirst());

        assertNull(cell(FIXTURE.resolveXlsx(our, their, Map.of(DRIVERS, Resolution.THEIRS)).getSheet(RULES), 8, 0));
        assertEquals(31, number(FIXTURE.resolveXlsx(our, their, Map.of(DRIVERS, Resolution.OURS)).getSheet(RULES),
                10, 1));
        assertEquals(30, number(FIXTURE.resolveXlsx(our, their, Map.of(DRIVERS, Resolution.BASE)).getSheet(RULES),
                10, 1));
    }

    @Test
    void tablesAddedInBothAtTheSamePlace_areTakenFromOneRevision() throws Exception {
        Consumer<Sheet> our = sheet -> set(sheet, 14, 0, "Data X x");
        Consumer<Sheet> their = sheet -> set(sheet, 14, 0, "Data Y y");
        var added = "Rules/added/A15";

        assertEquals(new TableConflict(added,
                CellRangeAddress.valueOf("A15"),
                null,
                CellRangeAddress.valueOf("A15"),
                TableConflictReason.ADDED_IN_BOTH), FIXTURE.conflicts(XSSFWorkbook::new, our, their)
                .sheets()
                .getFirst()
                .tables()
                .getFirst());

        assertEquals("Data Y y",
                text(FIXTURE.resolveXlsx(our, their, Map.of(added, Resolution.THEIRS)).getSheet(RULES), 14, 0));
        assertEquals("Data X x",
                text(FIXTURE.resolveXlsx(our, their, Map.of(added, Resolution.OURS)).getSheet(RULES), 14, 0));
        assertNull(cell(FIXTURE.resolveXlsx(our, their, Map.of(added, Resolution.BASE)).getSheet(RULES), 14, 0));
    }

    @Test
    void sheetRemovedInTheirs_andChangedInOurs_isResolvedAsAWhole() throws Exception {
        Consumer<Sheet> our = sheet -> set(sheet, 3, 1, 10);
        Consumer<Sheet> their = sheet -> sheet.getWorkbook().removeSheetAt(sheet.getWorkbook().getSheetIndex(sheet));

        assertEquals(List.of(new SheetConflicts(RULES,
                DiffStatus.CONFLICT,
                SheetConflictReason.REMOVED_AND_CHANGED,
                true,
                true,
                false,
                List.of())), TWO_SHEETS.conflicts(XSSFWorkbook::new, our, their).sheets());

        assertNull(TWO_SHEETS.resolveXlsx(our, their, Map.of(RULES, Resolution.THEIRS)).getSheet(RULES));
        assertEquals(10, number(TWO_SHEETS.resolveXlsx(our, their, Map.of(RULES, Resolution.OURS)).getSheet(RULES),
                3, 1));
        assertEquals(1, number(TWO_SHEETS.resolveXlsx(our, their, Map.of(RULES, Resolution.BASE)).getSheet(RULES),
                3, 1));
    }

    @Test
    void sheetRemovedInOneRevision_andFormattedInTheOther_isRemoved() throws Exception {
        Consumer<Sheet> remove = sheet -> sheet.getWorkbook().removeSheetAt(sheet.getWorkbook().getSheetIndex(sheet));
        Consumer<Sheet> format = sheet -> {
            bold(sheet, 3, 1);
            sheet.setColumnWidth(1, 9000);
        };

        assertNull(TWO_SHEETS.mergeXlsx(format, remove));
        assertNull(TWO_SHEETS.mergeXlsx(remove, format));
    }

    /** A hyperlink or the visibility of a sheet is no formatting, even when it comes with some. */
    @Test
    void sheetRemovedInOneRevision_andGivenOtherSettingsInTheOther_staysAConflict() throws Exception {
        Consumer<Sheet> remove = sheet -> sheet.getWorkbook().removeSheetAt(sheet.getWorkbook().getSheetIndex(sheet));
        Consumer<Sheet> link = sheet -> {
            bold(sheet, 3, 1);
            var hyperlink = sheet.getWorkbook().getCreationHelper().createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress("https://openl-tablets.org");
            CellUtil.getCell(CellUtil.getRow(3, sheet), 1).setHyperlink(hyperlink);
        };
        Consumer<Sheet> hide = sheet -> {
            bold(sheet, 3, 1);
            sheet.getWorkbook().setSheetHidden(sheet.getWorkbook().getSheetIndex(sheet), true);
        };

        assertTrue(TWO_SHEETS.diff(XSSFWorkbook::new, link, remove).hasConflicts());
        assertTrue(TWO_SHEETS.diff(XSSFWorkbook::new, remove, hide).hasConflicts());
    }

    @Test
    void sheetAddedInBothWithOtherVisibility_staysAConflict() throws Exception {
        Consumer<Sheet> our = sheet -> {
            var workbook = sheet.getWorkbook();
            set(workbook.createSheet("Extra"), 0, 0, "same");
            workbook.setSheetHidden(workbook.getSheetIndex("Extra"), true);
        };
        Consumer<Sheet> their = sheet -> set(sheet.getWorkbook().createSheet("Extra"), 0, 0, "same");

        assertTrue(FIXTURE.diff(XSSFWorkbook::new, our, their).hasConflicts());
    }

    @Test
    void sheetAddedInBothWithOtherFormatting_keepsOurs() throws Exception {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            var extra = sheet.getWorkbook().createSheet("Extra");
            set(extra, 0, 0, "same");
            bold(extra, 0, 0);
        }, sheet -> set(sheet.getWorkbook().createSheet("Extra"), 0, 0, "same")).getWorkbook().getSheet("Extra");

        assertEquals("same", text(merged, 0, 0));
        assertTrue(isBold(merged, 0, 0));
    }

    @Test
    void sheetRemovedInOurs_andChangedInTheirs_isAddedBackFromTheirs() throws Exception {
        Consumer<Sheet> our = sheet -> sheet.getWorkbook().removeSheetAt(sheet.getWorkbook().getSheetIndex(sheet));
        Consumer<Sheet> their = sheet -> set(sheet, 3, 1, 20);

        var merged = TWO_SHEETS.resolveXlsx(our, their, Map.of(RULES, Resolution.THEIRS));
        assertEquals(20, number(merged.getSheet(RULES), 3, 1));
        assertEquals("kept", text(merged.getSheet("Other"), 0, 0));
    }

    @Test
    void sheetAddedInBoth_isResolvedAsAWhole() throws Exception {
        Consumer<Sheet> our = sheet -> set(sheet.getWorkbook().createSheet("Extra"), 0, 0, "ours");
        Consumer<Sheet> their = sheet -> set(sheet.getWorkbook().createSheet("Extra"), 0, 0, "theirs");

        assertEquals(List.of(new SheetConflicts("Extra",
                DiffStatus.CONFLICT,
                SheetConflictReason.ADDED_IN_BOTH,
                true,
                false,
                true,
                List.of())), FIXTURE.conflicts(XSSFWorkbook::new, our, their).sheets());

        assertEquals("theirs",
                text(FIXTURE.resolveXlsx(our, their, Map.of("Extra", Resolution.THEIRS)).getSheet("Extra"), 0, 0));
        assertNull(FIXTURE.resolveXlsx(our, their, Map.of("Extra", Resolution.BASE)).getSheet("Extra"));
    }

    @Test
    void sheetTakenWhole_takesTheLayoutOfThatVersion() throws Exception {
        Consumer<Sheet> our = sheet -> {
            set(sheet, 3, 1, 10);
            sheet.setColumnHidden(2, true);
            // An empty column past the cells of every version.
            sheet.setColumnHidden(20, true);
            sheet.setDefaultColumnWidth(12);
        };
        Consumer<Sheet> their = sheet -> {
            set(sheet, 10, 1, 31);
            sheet.setColumnWidth(2, 5000);
            var toTheEnd = layOut(sheet, 31, 16384);
            toTheEnd.setWidth(20);
            toTheEnd.setCustomWidth(true);
            sheet.createRow(6).setZeroHeight(true);
            sheet.createFreezePane(0, 1);
        };

        var sheet = FIXTURE.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst();
        assertEquals(SheetConflictReason.SETTINGS, sheet.reason());
        assertEquals(List.of(), sheet.tables());

        var theirs = FIXTURE.resolveXlsx(our, their, Map.of(RULES, Resolution.THEIRS)).getSheet(RULES);
        assertFalse(theirs.isColumnHidden(2));
        assertEquals(5000, theirs.getColumnWidth(2));
        assertFalse(theirs.isColumnHidden(20));
        assertEquals(20 * 256, theirs.getColumnWidth(16383));
        assertTrue(theirs.getRow(6).getZeroHeight());
        assertEquals(8, theirs.getDefaultColumnWidth());
        var base = FIXTURE.resolveXlsx(our, their, Map.of(RULES, Resolution.BASE)).getSheet(RULES);
        assertFalse(base.isColumnHidden(2));
        assertFalse(base.isColumnHidden(20));
        assertEquals(8, base.getDefaultColumnWidth());
    }

    @Test
    void conflictingTables_areListedFromTopToBottom() throws Exception {
        Consumer<Sheet> our = sheet -> {
            set(sheet, 10, 1, 31);
            set(sheet, 0, 7, "Data X x");
        };
        Consumer<Sheet> their = sheet -> {
            set(sheet, 10, 1, 32);
            set(sheet, 0, 7, "Data Y y");
        };

        assertEquals(List.of("Rules/added/H1", DRIVERS), FIXTURE.conflicts(XSSFWorkbook::new, our, their)
                .sheets()
                .getFirst()
                .tables()
                .stream()
                .map(TableConflict::id)
                .toList());
    }

    @Test
    void tablesApartByRowsWithoutValues_areTablesOfTheirOwnWhateverTheRowsAreFilledWith() throws Exception {
        // The rows between the rules table and the data table hold no value, only the fill of the sheet.
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            var sheet = workbook.getSheet(RULES);
            for (var row = 6; row < 8; row++) {
                for (var column = 0; column < 3; column++) {
                    fill(cellOf(sheet, row, column), IndexedColors.GREY_25_PERCENT);
                }
            }
        });
        Consumer<Sheet> our = sheet -> set(sheet, 3, 1, 10);
        Consumer<Sheet> their = sheet -> {
            set(sheet, 3, 1, 20);
            set(sheet, 10, 1, 31);
            fill(cellOf(sheet, 6, 0), IndexedColors.LIGHT_YELLOW);
        };

        assertEquals(List.of(RATES), fixture.conflicts(XSSFWorkbook::new, our, their)
                .sheets()
                .getFirst()
                .tables()
                .stream()
                .map(TableConflict::id)
                .toList());

        var merged = fixture.resolveXlsx(our, their, Map.of(RATES, Resolution.OURS)).getSheet(RULES);
        assertEquals(10, number(merged, 3, 1));
        // The data table merges by itself, and so does the fill of a cell between the tables.
        assertEquals(31, number(merged, 10, 1));
        assertEquals(IndexedColors.LIGHT_YELLOW.getIndex(), cell(merged, 6, 0).getCellStyle().getFillForegroundColor());
        assertEquals(IndexedColors.GREY_25_PERCENT.getIndex(),
                cell(merged, 6, 1).getCellStyle().getFillForegroundColor());
    }

    private static void fill(Cell cell, IndexedColors color) {
        var style = cell.getSheet().getWorkbook().createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);
    }

    @Test
    void sheetDecidedAsAWhole_takesItsTablesWithIt() throws Exception {
        var merged = FIXTURE.resolveXlsx(OUR_RATE, THEIR_RATE, Map.of(RULES, Resolution.THEIRS)).getSheet(RULES);

        assertEquals(20, number(merged, 3, 1));
        assertEquals("model", text(merged, 1, 5));
        assertEquals(31, number(merged, 10, 1));
    }

    @Test
    void everyConflictingTableOfTheSheet_isListedAndResolvedOnItsOwn() throws Exception {
        Consumer<Sheet> our = sheet -> {
            set(sheet, 3, 1, 10);
            set(sheet, 10, 1, 31);
        };
        Consumer<Sheet> their = sheet -> {
            set(sheet, 3, 1, 20);
            removeTable(sheet, "A9:C12", "A9:C9");
        };

        var tables = FIXTURE.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst().tables();
        assertEquals(List.of(RATES, DRIVERS), tables.stream().map(TableConflict::id).toList());
        assertEquals(List.of(TableConflictReason.CELLS, TableConflictReason.REMOVED_AND_CHANGED),
                tables.stream().map(TableConflict::reason).toList());

        var merged = FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.THEIRS, DRIVERS, Resolution.OURS))
                .getSheet(RULES);
        assertEquals(20, number(merged, 3, 1));
        assertEquals(31, number(merged, 10, 1));
        var e = assertThrows(UnresolvedConflictsException.class,
                () -> FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.THEIRS)));
        assertEquals(List.of(DRIVERS), e.getUnresolved());
    }

    @Test
    void conflictWithoutDecision_isRefused() {
        var e = assertThrows(UnresolvedConflictsException.class,
                () -> FIXTURE.resolveXlsx(OUR_RATE, THEIR_RATE, Map.of()));

        assertEquals(List.of(RATES), e.getUnresolved());
        assertEquals(Map.of(), e.getRefused());
    }

    @Test
    void decisionThatMakesTablesTouch_isRefused() throws Exception {
        Consumer<Sheet> our = sheet -> {
            set(sheet, 3, 1, 10);
            set(sheet, 6, 0, "UK");
            set(sheet, 6, 1, 4);
        };
        Consumer<Sheet> their = sheet -> {
            set(sheet, 3, 1, 20);
            set(sheet, 7, 0, "Data Z z");
        };

        var sheet = NO_DATA.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst();
        assertEquals(List.of(RATES), sheet.tables().stream().map(TableConflict::id).toList());

        var e = assertThrows(UnresolvedConflictsException.class,
                () -> NO_DATA.resolveXlsx(our, their, Map.of(RATES, Resolution.OURS)));
        assertEquals(Map.of(RULES, SheetConflictReason.TOUCHING_TABLES), e.getRefused());

        var merged = NO_DATA.resolveXlsx(our, their, Map.of(RATES, Resolution.THEIRS)).getSheet(RULES);
        assertEquals(20, number(merged, 3, 1));
        assertNull(cell(merged, 6, 0));
        assertEquals("Data Z z", text(merged, 7, 0));
    }

    @Test
    void tableTheOtherRevisionShifted_landsWhereTheMergePutsItWhicheverVersionIsTaken() throws Exception {
        // THEIR revision grows the rules table and moves the data table below it two rows down, as Excel does.
        Consumer<Sheet> their = sheet -> {
            moveTable(sheet, "A9:C12", "A9:C9", 2, 0);
            insertRow(sheet, 4, "BR", 4);
            insertRow(sheet, 4, "AR", 5);
            set(sheet, 12, 1, 32);
        };
        Consumer<Sheet> our = sheet -> set(sheet, 10, 1, 31);

        assertEquals(List.of(DRIVERS), FIXTURE.conflicts(XSSFWorkbook::new, our, their)
                .sheets()
                .getFirst()
                .tables()
                .stream()
                .map(TableConflict::id)
                .toList());
        for (var age : Map.of(Resolution.OURS, 31.0, Resolution.BASE, 30.0, Resolution.THEIRS, 32.0).entrySet()) {
            var merged = FIXTURE.resolveXlsx(our, their, Map.of(DRIVERS, age.getKey())).getSheet(RULES);
            assertEquals("AR", text(merged, 4, 0));
            assertNull(cell(merged, 8, 0), age.getKey().name());
            assertEquals("Data Driver drivers", text(merged, 10, 0), age.getKey().name());
            assertEquals(age.getValue(), number(merged, 12, 1), age.getKey().name());
        }
    }

    @Test
    void refusalNoVersionOfTheTablesLifts_leavesTheSheetToBeResolvedAsAWhole() throws Exception {
        // OUR revision refers to the age of Ann, which THEIR revision moves with its table.
        Consumer<Sheet> our = sheet -> {
            set(sheet, 3, 1, 10);
            cellOf(sheet, 4, 4).setCellFormula("B11");
        };
        Consumer<Sheet> their = sheet -> {
            set(sheet, 3, 1, 20);
            moveTable(sheet, "A9:C12", "A9:C9", 2, 0);
        };

        var sheet = FIXTURE.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst();
        assertEquals(SheetConflictReason.MOVED_CONTENT, sheet.reason());
        assertEquals(List.of(), sheet.tables());

        var e = assertThrows(UnresolvedConflictsException.class,
                () -> FIXTURE.resolveXlsx(our, their, Map.of(RATES, Resolution.THEIRS)));
        assertEquals(Map.of(RULES, SheetConflictReason.MOVED_CONTENT), e.getRefused());
    }

    @Test
    void tableTakenFromBase_isRefusedWhenItsFormulaRefersToMovedContent() throws Exception {
        var fixture = new MergeFixture(workbook -> {
            tables(workbook);
            // The rate of MX is the age of Ann.
            cellOf(workbook.getSheet(RULES), 5, 1).setCellFormula("B11");
        });
        Consumer<Sheet> our = sheet -> {
            set(sheet, 3, 1, 10);
            moveTable(sheet, "A9:C12", "A9:C9", 2, 0);
        };
        Consumer<Sheet> their = sheet -> set(sheet, 3, 1, 20);

        var e = assertThrows(UnresolvedConflictsException.class,
                () -> fixture.resolveXlsx(our, their, Map.of(RATES, Resolution.BASE)));
        assertEquals(Map.of(RULES, SheetConflictReason.MOVED_CONTENT), e.getRefused());
        assertEquals(10, number(fixture.resolveXlsx(our, their, Map.of(RATES, Resolution.OURS)).getSheet(RULES), 3, 1));
    }

    @Test
    void tablesTheirRevisionAddedNextToOneOfOurs_areOneConflict() throws Exception {
        Consumer<Sheet> our = sheet -> {
            for (var row = 0; row < 4; row++) {
                set(sheet, row, 7, "ours " + row);
            }
        };
        Consumer<Sheet> their = sheet -> {
            set(sheet, 0, 7, "first");
            set(sheet, 1, 7, "first");
            set(sheet, 3, 7, "second");
            set(sheet, 4, 7, "second");
        };
        var added = "Rules/added/H1:H2";

        assertEquals(List.of(new TableConflict(added,
                CellRangeAddress.valueOf("H1:H4"),
                null,
                CellRangeAddress.valueOf("H1:H5"),
                TableConflictReason.ADDED_IN_BOTH)), FIXTURE.conflicts(XSSFWorkbook::new, our, their)
                .sheets()
                .getFirst()
                .tables());

        var theirs = FIXTURE.resolveXlsx(our, their, Map.of(added, Resolution.THEIRS)).getSheet(RULES);
        assertEquals("first", text(theirs, 0, 7));
        assertNull(cell(theirs, 2, 7));
        assertEquals("second", text(theirs, 4, 7));
        var ours = FIXTURE.resolveXlsx(our, their, Map.of(added, Resolution.OURS)).getSheet(RULES);
        assertEquals("ours 3", text(ours, 3, 7));
        assertNull(cell(ours, 4, 7));
        assertNull(cell(FIXTURE.resolveXlsx(our, their, Map.of(added, Resolution.BASE)).getSheet(RULES), 0, 7));
    }

    @Test
    void tablesTouchingWithoutAConflictOfTheirOwn_leaveTheSheetToBeResolvedAsAWhole() throws Exception {
        Consumer<Sheet> our = sheet -> {
            set(sheet, 6, 0, "UK");
            set(sheet, 6, 1, 4);
        };
        Consumer<Sheet> their = sheet -> set(sheet, 7, 0, "Data Z z");

        assertEquals(SheetConflictReason.TOUCHING_TABLES,
                NO_DATA.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst().reason());
        var merged = NO_DATA.resolveXlsx(our, their, Map.of(RULES, Resolution.OURS)).getSheet(RULES);
        assertEquals("UK", text(merged, 6, 0));
        assertNull(cell(merged, 7, 0));
    }

    @Test
    void sheetsChangedByOneRevision_areListedWithTheRevisionTheyAreTakenFrom() throws IOException {
        var conflicts = TWO_SHEETS.conflicts(XSSFWorkbook::new,
                sheet -> set(sheet, 3, 1, 10),
                sheet -> set(sheet.getWorkbook().getSheet("Other"), 0, 0, "theirs"));

        assertEquals(List.of(RULES, "Other"), conflicts.sheets().stream().map(SheetConflicts::name).toList());
        assertEquals(List.of(DiffStatus.OUR, DiffStatus.THEIR),
                conflicts.sheets().stream().map(SheetConflicts::status).toList());
        assertFalse(conflicts.palette());
    }

    @Test
    void xlsPaletteChangedDifferently_isTakenFromTheRevisionChosen() throws Exception {
        var index = HSSFColor.HSSFColorPredefined.LIME.getIndex();
        Consumer<Sheet> our = sheet -> palette(sheet).setColorAtIndex(index, (byte) 1, (byte) 2, (byte) 3);
        Consumer<Sheet> their = sheet -> palette(sheet).setColorAtIndex(index, (byte) 4, (byte) 5, (byte) 6);

        var conflicts = FIXTURE.conflicts(HSSFWorkbook::new, our, their);
        assertTrue(conflicts.palette());

        var merged = (HSSFWorkbook) FIXTURE.resolve(HSSFWorkbook::new,
                our,
                their,
                Map.of(WorkbookConflicts.PALETTE_ID, Resolution.THEIRS));
        assertArrayEquals(new short[]{4, 5, 6}, merged.getCustomPalette().getColor(index).getTriplet());
        var kept = (HSSFWorkbook) FIXTURE.resolve(HSSFWorkbook::new,
                our,
                their,
                Map.of(WorkbookConflicts.PALETTE_ID, Resolution.OURS));
        assertArrayEquals(new short[]{1, 2, 3}, kept.getCustomPalette().getColor(index).getTriplet());
    }

    private static org.apache.poi.hssf.usermodel.HSSFPalette palette(Sheet sheet) {
        return ((HSSFWorkbook) sheet.getWorkbook()).getCustomPalette();
    }

    /**
     * Checks the countries of the rules table from its first row of values, and that the table ends after them.
     */
    private static void assertCountries(Workbook workbook, String... countries) {
        var sheet = workbook.getSheet(RULES);
        for (var i = 0; i < countries.length; i++) {
            assertEquals(countries[i], text(sheet, 3 + i, 0));
        }
        var after = cell(sheet, 3 + countries.length, 0);
        assertTrue(after == null || after.getCellType() == CellType.BLANK);
    }

    private static List<CellRangeAddress> sorted(Sheet sheet) {
        return sheet.getMergedRegions()
                .stream()
                .sorted((a, b) -> a.getFirstRow() != b.getFirstRow()
                        ? Integer.compare(a.getFirstRow(), b.getFirstRow())
                        : Integer.compare(a.getFirstColumn(), b.getFirstColumn()))
                .toList();
    }
}
