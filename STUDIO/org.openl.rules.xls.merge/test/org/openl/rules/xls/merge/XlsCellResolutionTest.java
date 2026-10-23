package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.xls.merge.MergeFixture.RULES;
import static org.openl.rules.xls.merge.MergeFixture.bold;
import static org.openl.rules.xls.merge.MergeFixture.cell;
import static org.openl.rules.xls.merge.MergeFixture.comment;
import static org.openl.rules.xls.merge.MergeFixture.deleteRow;
import static org.openl.rules.xls.merge.MergeFixture.insertRow;
import static org.openl.rules.xls.merge.MergeFixture.isBold;
import static org.openl.rules.xls.merge.MergeFixture.number;
import static org.openl.rules.xls.merge.MergeFixture.set;
import static org.openl.rules.xls.merge.MergeFixture.text;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.xls.merge.diff.CellConflict;
import org.openl.rules.xls.merge.diff.CellPart;
import org.openl.rules.xls.merge.diff.CellResolution;
import org.openl.rules.xls.merge.diff.CellResolutionException;
import org.openl.rules.xls.merge.diff.CellSource;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.TableCell;
import org.openl.rules.xls.merge.diff.TableCells;
import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.TableConflictReason;
import org.openl.rules.xls.merge.diff.UnresolvedConflictsException;

class XlsCellResolutionTest {

    private static final String RATES = "Rules/A1:B6";
    private static final String DRIVERS = "Rules/A9:C12";
    private static final MergeFixture FIXTURE = new MergeFixture(MergeFixture::tables);

    /**
     * Both revisions change the age of Ann. OUR revision notes Bob as reviewed, THEIR revision changes his age.
     */
    private static final Consumer<Sheet> OUR_AGE = sheet -> {
        set(sheet, 10, 1, 31);
        set(sheet, 11, 2, "reviewed");
    };
    private static final Consumer<Sheet> THEIR_AGE = sheet -> {
        set(sheet, 10, 1, 32);
        set(sheet, 11, 1, 41);
    };
    /**
     * Both revisions change the age of Ann, and the name and the age of Bob.
     */
    private static final Consumer<Sheet> OUR_ROWS = sheet -> {
        set(sheet, 10, 1, 31);
        set(sheet, 11, 0, "Robert");
        set(sheet, 11, 1, 42);
    };
    private static final Consumer<Sheet> THEIR_ROWS = sheet -> {
        set(sheet, 10, 1, 32);
        set(sheet, 11, 0, "Rob");
        set(sheet, 11, 1, 41);
    };

    @Test
    void survey_listsTheMergedTableWithItsConflictingCells() throws IOException {
        var table = drivers(FIXTURE.conflicts(XSSFWorkbook::new, OUR_AGE, THEIR_AGE).sheets().getFirst().tables());

        assertEquals(TableConflictReason.CELLS, table.reason());
        var cells = Objects.requireNonNull(table.cells());
        assertEquals(CellRangeAddress.valueOf("A9:C12"), cells.place());
        assertEquals(List.of(CellRangeAddress.valueOf("A9:C9")), cells.regions());
        var conflict = cells.conflicts().getFirst();
        assertEquals(1, cells.conflicts().size());
        assertEquals(new CellAddress("B11"), conflict.cell());
        assertEquals(new CellAddress("B11"), conflict.our());
        assertEquals(new CellAddress("B11"), conflict.base());
        assertEquals(new CellAddress("B11"), conflict.their());
        assertEquals(Set.of(), conflict.ourSource().parts());
        assertEquals(Set.of(CellPart.VALUE), conflict.theirSource().parts());
        // the cells only one revision changed are merged already
        assertEquals(Set.of(CellPart.VALUE), source(cells, "B12").parts());
        assertEquals(Set.of(), source(cells, "C12").parts());
        assertEquals(new CellAddress("C12"), source(cells, "C12").our());
        // their age of Bob and our note on him are the changes the merge takes by itself, the age of Ann conflicts
        assertEquals(List.of(new CellAddress("B12"), new CellAddress("C12")), cells.changed());
    }

    @Test
    void tableWithRowsInsertedByOneRevision_isMergedCellByCellAtItsMergedRows() throws Exception {
        Consumer<Sheet> ours = sheet -> {
            insertRow(sheet, 4, "UK", 5);
            set(sheet, 3, 1, 10);
        };
        Consumer<Sheet> theirs = sheet -> {
            set(sheet, 3, 1, 20);
            set(sheet, 5, 1, 30);
        };
        var table = rates(FIXTURE.conflicts(XSSFWorkbook::new, ours, theirs).sheets().getFirst().tables());
        var cells = Objects.requireNonNull(table.cells());
        assertEquals(CellRangeAddress.valueOf("A1:B7"), cells.place());
        assertEquals(List.of(new CellAddress("B4")), cells.conflicts().stream().map(CellConflict::cell).toList());
        // the row our revision inserted, and their rate of MX it pushed down
        assertEquals(List.of(new CellAddress("A5"), new CellAddress("B5"), new CellAddress("B7")), cells.changed());

        var merged = FIXTURE.resolveByCells(ours, theirs, Map.of(RATES, chosen(3, 1, Resolution.THEIRS)))
                .getSheet(RULES);

        assertEquals(20, number(merged, 3, 1));
        assertEquals("UK", text(merged, 4, 0));
        assertEquals(5, number(merged, 4, 1));
        assertEquals(30, number(merged, 6, 1));
    }

    @Test
    void conflictingCellTakenFromTheirs_keepsTheOtherChangesOfBoth() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, chosen(2, 1, Resolution.THEIRS)))
                .getSheet(RULES);

        assertEquals(32, number(merged, 10, 1));
        assertEquals(41, number(merged, 11, 1));
        assertEquals("reviewed", text(merged, 11, 2));
    }

    @Test
    void conflictingCellTakenFromOurs_keepsTheOtherChangesOfBoth() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, chosen(2, 1, Resolution.OURS)))
                .getSheet(RULES);

        assertEquals(31, number(merged, 10, 1));
        assertEquals(41, number(merged, 11, 1));
        assertEquals("reviewed", text(merged, 11, 2));
    }

    @Test
    void tableChoice_givesEveryConflictingCellItsVersion() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_ROWS, THEIR_ROWS, Map.of(DRIVERS,
                new CellResolution(Resolution.OURS, Map.of(), Map.of()))).getSheet(RULES);

        assertEquals(31, number(merged, 10, 1));
        assertEquals("Robert", text(merged, 11, 0));
        assertEquals(42, number(merged, 11, 1));
    }

    @Test
    void cellChoice_overridesTheChoiceOfTheTable() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_ROWS, THEIR_ROWS, Map.of(DRIVERS, new CellResolution(Resolution.THEIRS,
                Map.of(new TableCell(3, 0), Resolution.OURS),
                Map.of()))).getSheet(RULES);

        assertEquals(32, number(merged, 10, 1));
        assertEquals("Robert", text(merged, 11, 0));
        assertEquals(41, number(merged, 11, 1));
    }

    @Test
    void typedValue_resolvesAConflictingCell() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, typed(2, 1, "31.5")))
                .getSheet(RULES);

        assertEquals(31.5, number(merged, 10, 1));
        assertEquals(41, number(merged, 11, 1));
        assertEquals("reviewed", text(merged, 11, 2));
    }

    @Test
    void typedValues_areReadAsExcelReadsThem() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, new CellResolution(Resolution.OURS,
                Map.of(),
                Map.of(new TableCell(2, 2), "=B12*2",
                        new TableCell(3, 2), "TRUE",
                        new TableCell(2, 0), "'42",
                        new TableCell(3, 0), "-1.5E2",
                        new TableCell(1, 2), "comment")))).getSheet(RULES);

        var formula = cell(merged, 10, 2);
        assertEquals(CellType.FORMULA, formula.getCellType());
        assertEquals("B12*2", formula.getCellFormula());
        assertEquals(82, formula.getNumericCellValue());
        assertTrue(cell(merged, 11, 2).getBooleanCellValue());
        assertEquals("42", text(merged, 10, 0));
        assertEquals(-150, number(merged, 11, 0));
        assertEquals("comment", text(merged, 9, 2));
    }

    @Test
    void typedDates_takeADateFormat() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, new CellResolution(Resolution.OURS,
                Map.of(),
                Map.of(new TableCell(2, 2), "2026-10-22", new TableCell(3, 2), "2026-10-22 14:30")))).getSheet(RULES);

        var date = cell(merged, 10, 2);
        assertTrue(DateUtil.isCellDateFormatted(date));
        assertEquals(LocalDate.of(2026, 10, 22), date.getLocalDateTimeCellValue().toLocalDate());
        var dateTime = cell(merged, 11, 2);
        assertTrue(DateUtil.isCellDateFormatted(dateTime));
        assertEquals(LocalDateTime.of(2026, 10, 22, 14, 30), dateTime.getLocalDateTimeCellValue());
    }

    @Test
    void emptyTypedValue_clearsTheCell() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, typed(2, 1, ""))).getSheet(RULES);

        assertNull(cell(merged, 10, 1));
    }

    @Test
    void typedValue_keepsTheFormattingOfTheMergedCell() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, sheet -> {
            THEIR_AGE.accept(sheet);
            bold(sheet, 10, 2);
        }, Map.of(DRIVERS, new CellResolution(Resolution.THEIRS, Map.of(),
                Map.of(new TableCell(2, 2), "checked")))).getSheet(RULES);

        assertEquals("checked", text(merged, 10, 2));
        assertTrue(isBold(merged, 10, 2));
    }

    @Test
    void conflictingCellTakenFromOurs_keepsTheStyleTheirRevisionAloneChanged() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, sheet -> {
            THEIR_AGE.accept(sheet);
            bold(sheet, 10, 1);
        }, Map.of(DRIVERS, chosen(2, 1, Resolution.OURS))).getSheet(RULES);

        assertEquals(31, number(merged, 10, 1));
        assertTrue(isBold(merged, 10, 1));
    }

    @Test
    void commentsChangedDifferently_conflictAndComeFromTheVersionChosen() throws Exception {
        Consumer<Sheet> ours = sheet -> comment(sheet, 10, 1, "E10:G12", "ours");
        Consumer<Sheet> theirs = sheet -> comment(sheet, 10, 1, "E10:G12", "theirs");
        var table = drivers(FIXTURE.conflicts(XSSFWorkbook::new, ours, theirs).sheets().getFirst().tables());
        assertEquals(List.of(new CellAddress("B11")),
                Objects.requireNonNull(table.cells()).conflicts().stream().map(CellConflict::cell).toList());

        var merged = FIXTURE.resolveByCells(ours, theirs, Map.of(DRIVERS, chosen(2, 1, Resolution.THEIRS)))
                .getSheet(RULES);

        assertEquals("theirs", cell(merged, 10, 1).getCellComment().getString().getString());
        assertEquals(30, number(merged, 10, 1));
    }

    @Test
    void conflictingCellLeftUndecided_leavesTheTableUnresolved() {
        var byCells = Map.of(DRIVERS, chosen(2, 1, Resolution.THEIRS));
        var e = assertThrows(UnresolvedConflictsException.class,
                () -> FIXTURE.resolveByCells(OUR_ROWS, THEIR_ROWS, byCells));

        assertEquals(List.of(DRIVERS), e.getUnresolved());
        assertTrue(e.getNotByCells().isEmpty());
    }

    @Test
    void tableThatCannotBeMergedCellByCell_isRefused() throws IOException {
        Consumer<Sheet> ours = sheet -> insertRow(sheet, 4, "UK", 5);
        Consumer<Sheet> theirs = sheet -> deleteRow(sheet, 4);
        var table = rates(FIXTURE.conflicts(XSSFWorkbook::new, ours, theirs).sheets().getFirst().tables());
        assertEquals(TableConflictReason.ROWS, table.reason());
        assertNull(table.cells());

        var byCells = Map.of(RATES, chosen(4, 1, Resolution.THEIRS));
        var e = assertThrows(UnresolvedConflictsException.class, () -> FIXTURE.resolveByCells(ours, theirs, byCells));

        assertEquals(List.of(RATES), e.getNotByCells());
        assertTrue(e.getUnresolved().isEmpty());
    }

    @Test
    void cellOutsideTheMergedTable_isRefused() {
        var byCells = Map.of(DRIVERS, chosen(4, 0, Resolution.OURS));
        var e = assertThrows(CellResolutionException.class, () -> FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, byCells));

        assertEquals(DRIVERS, e.getConflict());
        assertEquals(CellResolutionException.Kind.OUTSIDE, e.getKind());
        assertEquals(4, e.getRow());
        assertEquals(0, e.getColumn());
    }

    @Test
    void valueTypedUnderMergedCells_isRefused() {
        var byCells = Map.of(DRIVERS, new CellResolution(Resolution.OURS, Map.of(),
                Map.of(new TableCell(0, 1), "header")));
        var e = assertThrows(CellResolutionException.class, () -> FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, byCells));

        assertEquals(CellResolutionException.Kind.COVERED, e.getKind());
        assertEquals(0, e.getRow());
        assertEquals(1, e.getColumn());
    }

    @Test
    void unreadableTypedFormula_isRefused() {
        var byCells = Map.of(DRIVERS, typed(2, 1, "=SUM("));
        var e = assertThrows(CellResolutionException.class, () -> FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, byCells));

        assertEquals(CellResolutionException.Kind.FORMULA, e.getKind());
    }

    @Test
    void typedFormulaOfNoSheet_isRefused() {
        var byCells = Map.of(DRIVERS, typed(2, 1, "=Rates2!A1"));
        var e = assertThrows(CellResolutionException.class, () -> FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, byCells));

        assertEquals(CellResolutionException.Kind.FORMULA, e.getKind());
        assertEquals(DRIVERS, e.getConflict());
        assertEquals(2, e.getRow());
        assertEquals(1, e.getColumn());
    }

    @ParameterizedTest
    @ValueSource(strings = {"=SUM(Rates2!A1:B2)", "=SUM(Rules:Rates2!A1)"})
    void typedFormulaOfNoSheetInAnAreaOrARange_isRefused(String formula) {
        var byCells = Map.of(DRIVERS, typed(2, 1, formula));
        assertThrows(CellResolutionException.class, () -> FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, byCells));
    }

    @Test
    void xlsTypedFormulaOfNoSheet_isRefused() {
        var byCells = Map.of(DRIVERS, typed(2, 1, "=Rates2!A1"));
        assertThrows(CellResolutionException.class,
                () -> FIXTURE.resolve(HSSFWorkbook::new, OUR_AGE, THEIR_AGE, Map.of(), byCells));
    }

    @Test
    void typedFormula_readsASheetTheirRevisionAdded() throws Exception {
        Consumer<Sheet> their = THEIR_AGE.andThen(sheet -> set(sheet.getWorkbook().createSheet("Discounts"), 0, 0, 5));

        var merged = FIXTURE.resolveByCells(OUR_AGE, their, Map.of(DRIVERS, typed(2, 1, "=Discounts!A1*2")))
                .getSheet(RULES);

        assertEquals("Discounts!A1*2", cell(merged, 10, 1).getCellFormula());
        assertEquals(10, number(merged, 10, 1));
    }

    @Test
    void typedDatesBeforeTheFirstDayOfTheWorkbook_stayText() throws Exception {
        var merged = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, new CellResolution(Resolution.OURS,
                Map.of(),
                Map.of(new TableCell(2, 2), "1899-12-31", new TableCell(3, 2), "1899-12-31 14:30")))).getSheet(RULES);

        assertEquals("1899-12-31", text(merged, 10, 2));
        assertEquals("1899-12-31 14:30", text(merged, 11, 2));
    }

    @Test
    void xlsTypedDates_startIn1900() throws Exception {
        var merged = FIXTURE.resolve(HSSFWorkbook::new, OUR_AGE, THEIR_AGE, Map.of(), Map.of(DRIVERS,
                new CellResolution(Resolution.OURS,
                        Map.of(),
                        Map.of(new TableCell(2, 2), "1899-12-31", new TableCell(3, 2), "1900-01-01")))).getSheet(RULES);

        assertEquals("1899-12-31", text(merged, 10, 2));
        assertTrue(DateUtil.isCellDateFormatted(cell(merged, 11, 2)));
    }

    @Test
    void typedDatesOfAWorkbookDatedFrom1904_startIn1904() throws Exception {
        var merged = FIXTURE.resolve(() -> {
            var workbook = new XSSFWorkbook();
            var ct = workbook.getCTWorkbook();
            (ct.isSetWorkbookPr() ? ct.getWorkbookPr() : ct.addNewWorkbookPr()).setDate1904(true);
            return workbook;
        }, OUR_AGE, THEIR_AGE, Map.of(), Map.of(DRIVERS, new CellResolution(Resolution.OURS,
                Map.of(),
                Map.of(new TableCell(2, 2), "1903-12-31", new TableCell(3, 2), "1904-01-01")))).getSheet(RULES);

        assertEquals("1903-12-31", text(merged, 10, 2));
        var date = cell(merged, 11, 2);
        assertTrue(DateUtil.isCellDateFormatted(date));
        assertEquals(LocalDate.of(1904, 1, 1), date.getLocalDateTimeCellValue().toLocalDate());
    }

    @Test
    void baseRevisionChosenForCells_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CellResolution(Resolution.BASE, Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> chosen(0, 0, Resolution.BASE));
    }

    @Test
    void tableDecidedBothWholeAndCellByCell_isRejected() {
        var decisions = Map.of(DRIVERS, Resolution.OURS);
        var byCells = Map.of(DRIVERS, chosen(2, 1, Resolution.OURS));
        assertThrows(IllegalArgumentException.class,
                () -> FIXTURE.resolve(XSSFWorkbook::new, OUR_AGE, THEIR_AGE, decisions, byCells));
    }

    @Test
    void tableDecidedWhole_isStillTakenWhole() throws Exception {
        var merged = FIXTURE.resolveXlsx(OUR_AGE, THEIR_AGE, Map.of(DRIVERS, Resolution.THEIRS)).getSheet(RULES);

        assertEquals(32, number(merged, 10, 1));
        assertEquals(41, number(merged, 11, 1));
        assertEquals("b", text(merged, 11, 2));
    }

    @Test
    void strictMerge_leavesTheTableAConflict() throws IOException {
        var diff = FIXTURE.diff(XSSFWorkbook::new, OUR_AGE, THEIR_AGE);

        assertTrue(diff.hasConflicts());
    }

    @Test
    void sameDecisionsTwice_giveTheSameWorkbook() throws Exception {
        var decisions = Map.of(DRIVERS, new CellResolution(Resolution.THEIRS,
                Map.of(),
                Map.of(new TableCell(2, 2), "=B12*2")));

        var first = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, decisions).getSheet(RULES);
        var second = FIXTURE.resolveByCells(OUR_AGE, THEIR_AGE, decisions).getSheet(RULES);

        assertFalse(XlsSheetsMatcher.hasChanges(first.getWorkbook(), first, second.getWorkbook(), second));
    }

    @Test
    void sideOf_takesTheCellThenTheTable() {
        var resolution = new CellResolution(Resolution.OURS, Map.of(new TableCell(1, 2), Resolution.THEIRS), Map.of());

        assertEquals(Resolution.THEIRS, resolution.sideOf(new TableCell(1, 2)));
        assertEquals(Resolution.OURS, resolution.sideOf(new TableCell(0, 2)));
        assertNull(new CellResolution(null, Map.of(), Map.of()).sideOf(new TableCell(0, 0)));
    }

    private static CellResolution chosen(int row, int column, Resolution side) {
        return new CellResolution(null, Map.of(new TableCell(row, column), side), Map.of());
    }

    private static CellResolution typed(int row, int column, String value) {
        return new CellResolution(null, Map.of(), Map.of(new TableCell(row, column), value));
    }

    private static TableConflict drivers(List<TableConflict> tables) {
        return tables.stream().filter(table -> DRIVERS.equals(table.id())).findFirst().orElseThrow();
    }

    private static TableConflict rates(List<TableConflict> tables) {
        return tables.stream().filter(table -> RATES.equals(table.id())).findFirst().orElseThrow();
    }

    private static CellSource source(TableCells cells, String address) {
        return Objects.requireNonNull(cells.cells().get(new CellAddress(address)));
    }
}
