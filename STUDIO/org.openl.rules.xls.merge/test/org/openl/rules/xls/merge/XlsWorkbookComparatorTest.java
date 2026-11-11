package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.xls.merge.compare.ChangeStatus;
import org.openl.rules.xls.merge.compare.LineChange;
import org.openl.rules.xls.merge.compare.SheetComparison;
import org.openl.rules.xls.merge.compare.TableComparison;
import org.openl.rules.xls.merge.compare.WorkbookComparison;

/**
 * Covers the comparison of two versions of a workbook: the tables it finds, the rows and the columns it lines up, and
 * the cells it marks.
 */
class XlsWorkbookComparatorTest {

    private static final String RULES = "Rules";
    private static final String RATE = "SmartRules Double rate(String country)";
    private static final String MIN_AGE = "SmartRules Integer minAge(String country)";
    private static final Object[][] RATES = {{"US", 1.0}, {"DE", 1.1}, {"FR", 1.2}, {"ES", 1.3}};
    private static final Object[][] AGES = {{"US", 16.0}, {"DE", 18.0}};
    private static final String HEADERS = "SmartRules Collect TableHeader[] TableHeadersByTableName(String tableID)";

    @TempDir
    Path folder;

    @Test
    void equalWorkbooks_holdEqualTables() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, XlsWorkbookComparatorTest::base)) {
            var sheet = sheet(comparison);
            assertEquals(ChangeStatus.EQUAL, sheet.status());
            assertEquals(List.of(ChangeStatus.EQUAL, ChangeStatus.EQUAL),
                    sheet.tables().stream().map(TableComparison::status).toList());
            var rate = table(comparison, RATE);
            assertTrue(rate.rows().stream().allMatch(line -> line.status() == ChangeStatus.EQUAL));
            assertEquals(6, rate.rows().size());
            assertTrue(rate.firstChanged().isEmpty());
        }
    }

    @Test
    void rowInsertedInTheMiddle_isAdded_andTheTableUnderIsPushedNotChanged() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, new Object[][]{{"US", 1.0}, {"IT", 1.5}, {"DE", 1.1}, {"FR", 1.2}, {"ES", 1.3}});
            ages(sheet, 9);
        })) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertFalse(rate.moved());
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL),
                    line(1, 1, ChangeStatus.EQUAL),
                    line(2, 2, ChangeStatus.EQUAL),
                    line(LineChange.NONE, 3, ChangeStatus.ADDED),
                    line(3, 4, ChangeStatus.EQUAL),
                    line(4, 5, ChangeStatus.EQUAL),
                    line(5, 6, ChangeStatus.EQUAL)), rate.rows());
            assertTrue(rate.firstChanged().isEmpty());
            assertTrue(rate.secondChanged().isEmpty());

            var minAge = table(comparison, MIN_AGE);
            assertEquals(ChangeStatus.EQUAL, minAge.status());
            assertFalse(minAge.moved());
            assertEquals(CellRangeAddress.valueOf("A9:B12"), minAge.first());
            assertEquals(CellRangeAddress.valueOf("A10:B13"), minAge.second());
        }
    }

    @Test
    void rowDeleted_andTheNextChanged_areRemovedAndChanged() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, new Object[][]{{"US", 1.0}, {"FR", 9.9}, {"ES", 1.3}});
            ages(sheet, 8);
        })) {
            var rate = table(comparison, RATE);
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL),
                    line(1, 1, ChangeStatus.EQUAL),
                    line(2, 2, ChangeStatus.EQUAL),
                    line(3, LineChange.NONE, ChangeStatus.REMOVED),
                    line(4, 3, ChangeStatus.CHANGED),
                    line(5, 4, ChangeStatus.EQUAL)), rate.rows());
            assertEquals(List.of(new CellAddress("B5")), rate.firstChanged());
            assertEquals(List.of(new CellAddress("B4")), rate.secondChanged());
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL), line(1, 1, ChangeStatus.CHANGED)), rate.columns());
        }
    }

    @Test
    void rowRemovedAndRowAdded_withTheRowsBetweenChanged_areRemovedChangedAndAdded() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, new Object[][]{{"US", 1.0}, {"FR", 1.25}, {"ES", 1.35}, {"IT", 4.0}});
            ages(sheet, 8);
        })) {
            var rate = table(comparison, RATE);
            // As many rows on both sides, yet the rows stand against the rows they were, not against those at their place
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL),
                    line(1, 1, ChangeStatus.EQUAL),
                    line(2, 2, ChangeStatus.EQUAL),
                    line(3, LineChange.NONE, ChangeStatus.REMOVED),
                    line(4, 3, ChangeStatus.CHANGED),
                    line(5, 4, ChangeStatus.CHANGED),
                    line(LineChange.NONE, 5, ChangeStatus.ADDED)), rate.rows());
            assertEquals(List.of(new CellAddress("B4"), new CellAddress("B5")), rate.secondChanged());
        }
    }

    @Test
    void rowsRewrittenWhole_areChangedInPlace() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, new Object[][]{{"US", 1.0}, {"IT", 2.0}, {"PL", 3.0}, {"ES", 1.3}});
            ages(sheet, 8);
        })) {
            var rate = table(comparison, RATE);
            // No row of the two is alike any other one, so each was rewritten where it stands
            assertEquals(List.of(line(3, 3, ChangeStatus.CHANGED), line(4, 4, ChangeStatus.CHANGED)),
                    rate.rows().subList(3, 5));
            assertEquals(6, rate.rows().size());
        }
    }

    @Test
    void columnInsertedWithANewParameter_isTheSameTable_withTheColumnAdded() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            set(sheet, 0, 0, "SmartRules Double rate(String country, String region)");
            set(sheet, 1, 0, "Country");
            set(sheet, 1, 1, "Region");
            set(sheet, 1, 2, "Rate");
            for (var i = 0; i < RATES.length; i++) {
                set(sheet, i + 2, 0, RATES[i][0]);
                set(sheet, i + 2, 1, "Any");
                set(sheet, i + 2, 2, RATES[i][1]);
            }
            ages(sheet, 8);
        })) {
            var sheet = sheet(comparison);
            assertEquals(2, sheet.tables().size());
            var rate = sheet.tables().getFirst();
            assertEquals("SmartRules Double rate(String country, String region)", rate.name());
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertEquals(List.of(line(0, 0, ChangeStatus.CHANGED),
                    line(LineChange.NONE, 1, ChangeStatus.ADDED),
                    line(1, 2, ChangeStatus.EQUAL)), rate.columns());
            assertEquals(List.of(new CellAddress("A1")), rate.firstChanged());
            assertEquals(ChangeStatus.CHANGED, rate.rows().getFirst().status());
            assertTrue(rate.rows().subList(1, 6).stream().allMatch(line -> line.status() == ChangeStatus.EQUAL));
        }
    }

    @Test
    void tableMovedOnItsSheet_isMoved() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, RATES);
            ages(sheet, 0, 3);
        })) {
            var minAge = table(comparison, MIN_AGE);
            assertEquals(ChangeStatus.MOVED, minAge.status());
            assertTrue(minAge.moved());
            assertEquals(CellRangeAddress.valueOf("A9:B12"), minAge.first());
            assertEquals(CellRangeAddress.valueOf("D1:E4"), minAge.second());
            assertEquals(ChangeStatus.CHANGED, sheet(comparison).status());
        }
    }

    @Test
    void tableMovedDownWithNothingAbove_isMoved() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, RATES);
            ages(sheet, 20);
        })) {
            assertEquals(ChangeStatus.MOVED, table(comparison, MIN_AGE).status());
        }
    }

    @Test
    void tableMovedAndChanged_isChanged_andMoved() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, RATES);
            rates(sheet, 20, MIN_AGE, new Object[][]{{"US", 16.0}, {"DE", 17.0}});
        })) {
            var minAge = table(comparison, MIN_AGE);
            assertEquals(ChangeStatus.CHANGED, minAge.status());
            assertTrue(minAge.moved());
        }
    }

    @Test
    void tableAddedAbove_pushesTheTableUnder_whichStaysEqual() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, RATES);
            rates(sheet, 8, "SmartRules Integer maxAge(String country)", new Object[][]{{"US", 80.0}});
            ages(sheet, 13);
        })) {
            assertEquals(ChangeStatus.EQUAL, table(comparison, MIN_AGE).status());
            assertEquals(ChangeStatus.ADDED, table(comparison, "SmartRules Integer maxAge(String country)").status());
            assertEquals(List.of(RATE, "SmartRules Integer maxAge(String country)", MIN_AGE),
                    sheet(comparison).tables().stream().map(TableComparison::name).toList());
        }
    }

    @Test
    void tableMovedDownPastATableAddedBelowWhereItStood_isMoved() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, RATES);
            rates(sheet, 20, "SmartRules Integer maxAge(String country)", new Object[][]{{"US", 80.0}});
            ages(sheet, 30);
        })) {
            // The added table stands in rows the moved one never stood in, so it did not push it there
            var minAge = table(comparison, MIN_AGE);
            assertEquals(ChangeStatus.MOVED, minAge.status());
            assertTrue(minAge.moved());
            assertEquals(CellRangeAddress.valueOf("A31:B34"), minAge.second());
        }
    }

    @Test
    void tableRemovedAbove_pullsTheTableUnder_whichStaysEqual() throws IOException {
        try (var comparison = compare(sheet -> {
            rates(sheet, 0, RATE, RATES);
            ages(sheet, 8);
            rates(sheet, 13, "SmartRules Integer maxAge(String country)", new Object[][]{{"US", 80.0}});
        }, sheet -> {
            ages(sheet, 0);
            rates(sheet, 5, "SmartRules Integer maxAge(String country)", new Object[][]{{"US", 80.0}});
        })) {
            assertEquals(ChangeStatus.REMOVED, table(comparison, RATE).status());
            assertNull(table(comparison, RATE).second());
            assertEquals(ChangeStatus.EQUAL, table(comparison, MIN_AGE).status());
            assertEquals(ChangeStatus.EQUAL, table(comparison, "SmartRules Integer maxAge(String country)").status());
        }
    }

    @Test
    void versionsOfOneRule_pushedByARowInserted_arePairedInTheirOrder() throws IOException {
        try (var comparison = compare(sheet -> {
            rates(sheet, 0, RATE, RATES);
            rates(sheet, 8, RATE, new Object[][]{{"US", 2.0}});
            rates(sheet, 13, RATE, new Object[][]{{"US", 3.0}});
        }, sheet -> {
            rates(sheet, 0, RATE, new Object[][]{{"US", 1.0}, {"IT", 1.5}, {"DE", 1.1}, {"FR", 1.2}, {"ES", 1.3}});
            rates(sheet, 9, RATE, new Object[][]{{"US", 2.0}});
            rates(sheet, 14, RATE, new Object[][]{{"US", 3.0}});
        })) {
            assertEquals(List.of(ChangeStatus.CHANGED, ChangeStatus.EQUAL, ChangeStatus.EQUAL),
                    sheet(comparison).tables().stream().map(TableComparison::status).toList());
        }
    }

    @Test
    void headerRenamed_isTheSameTable_withTheHeaderChanged() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, "SmartRules Double baseRate(String country)", RATES);
            ages(sheet, 8);
        })) {
            var rate = table(comparison, "SmartRules Double baseRate(String country)");
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertEquals(List.of(new CellAddress("A1")), rate.firstChanged());
            assertEquals(List.of(new CellAddress("A1")), rate.secondChanged());
        }
    }

    @Test
    void headerRenamedAndTableMoved_isTheSameTable_whenItKeepsMostOfItsValues() throws IOException {
        var renamed = "SmartRules Double baseRate(String country)";
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            ages(sheet, 0);
            rates(sheet, 10, renamed, new Object[][]{{"US", 1.0}, {"DE", 1.15}, {"FR", 1.2}, {"ES", 1.3}, {"IT", 1.5}});
        })) {
            var rate = table(comparison, renamed);
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertTrue(rate.moved());
            assertEquals(CellRangeAddress.valueOf("A1:B6"), rate.first());
            assertEquals(CellRangeAddress.valueOf("A11:B17"), rate.second());
            assertEquals(2, sheet(comparison).tables().size());
        }
    }

    @Test
    void anotherRuleOfTheSameSignatureAddedWhereOneWasRemoved_isAnotherTable() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            ages(sheet, 0);
            rates(sheet, 10, "SmartRules Double fee(String country)", new Object[][]{{"PL", 9.0}, {"SE", 8.0}});
        })) {
            assertEquals(ChangeStatus.REMOVED, table(comparison, RATE).status());
            assertEquals(ChangeStatus.ADDED, table(comparison, "SmartRules Double fee(String country)").status());
        }
    }

    @Test
    void tableOfAnotherKindPutWhereATableWas_isAnotherTable() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, "Datatype Rate", new Object[][]{{"String", "country"}, {"Double", "rate"}});
            ages(sheet, 8);
        })) {
            assertEquals(ChangeStatus.REMOVED, table(comparison, RATE).status());
            assertEquals(ChangeStatus.ADDED, table(comparison, "Datatype Rate").status());
        }
    }

    @Test
    void cellsOutsideOpenLTablesKeptInPlace_areTheSameCellsChanged() throws IOException {
        var formula = "TEXTJOIN(\",\",TRUE,A2:A9)";
        var rewritten = "_xludf.TEXTJOIN(\",\",TRUE,A2:A9)";
        try (var comparison = compare(sheet -> {
            base(sheet);
            set(sheet, 20, 8, formula);
        }, sheet -> {
            base(sheet);
            set(sheet, 20, 8, rewritten);
        })) {
            assertEquals(ChangeStatus.CHANGED, table(comparison, rewritten).status());
        }
    }

    @Test
    void rowsAndColumnsChanged_pairTheColumnsByTheirValues() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            set(sheet, 0, 0, RATE);
            set(sheet, 1, 0, "Country");
            set(sheet, 1, 1, "Region");
            set(sheet, 1, 2, "Rate");
            var rows = new Object[][]{{"US", 1.0}, {"IT", 1.5}, {"DE", 1.1}, {"FR", 1.2}, {"ES", 1.3}};
            for (var i = 0; i < rows.length; i++) {
                set(sheet, i + 2, 0, rows[i][0]);
                set(sheet, i + 2, 1, "Any");
                set(sheet, i + 2, 2, rows[i][1]);
            }
            ages(sheet, 9);
        })) {
            var rate = table(comparison, RATE);
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL),
                    line(LineChange.NONE, 1, ChangeStatus.ADDED),
                    line(1, 2, ChangeStatus.EQUAL)), rate.columns());
            assertEquals(line(LineChange.NONE, 3, ChangeStatus.ADDED), rate.rows().get(3));
            assertEquals(7, rate.rows().size());
            assertTrue(rate.firstChanged().isEmpty());
        }
    }

    @Test
    void rowsSorted_areRemovedAndAdded_whereTheyMoved() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            rates(sheet, 0, RATE, new Object[][]{{"ES", 1.3}, {"FR", 1.2}, {"DE", 1.1}, {"US", 1.0}});
            ages(sheet, 8);
        })) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.CHANGED, rate.status());
            // one country keeps its place among the others, as a line does in a text comparison
            assertEquals(List.of(ChangeStatus.EQUAL, ChangeStatus.EQUAL),
                    rate.rows().subList(0, 2).stream().map(LineChange::status).toList());
            assertEquals(3, count(rate.rows(), ChangeStatus.EQUAL));
            assertEquals(3, count(rate.rows(), ChangeStatus.REMOVED));
            assertEquals(3, count(rate.rows(), ChangeStatus.ADDED));
            assertTrue(rate.firstChanged().isEmpty());
        }
    }

    @Test
    void valueOfAVocabularyReplacedInPlace_isChanged() throws IOException {
        var header = "Datatype Species <String>";
        try (var comparison = compare(sheet -> {
            set(sheet, 0, 0, header);
            set(sheet, 1, 0, "Dog");
            set(sheet, 2, 0, "Cat");
            set(sheet, 3, 0, "Parrot");
        }, sheet -> {
            set(sheet, 0, 0, header);
            set(sheet, 1, 0, "Dog");
            set(sheet, 2, 0, "Bird");
            set(sheet, 3, 0, "Parrot");
        })) {
            var species = table(comparison, header);
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL),
                    line(1, 1, ChangeStatus.EQUAL),
                    line(2, 2, ChangeStatus.CHANGED),
                    line(3, 3, ChangeStatus.EQUAL)), species.rows());
        }
    }

    @Test
    void valueOfAVocabularyReplacedNextToAValueAdded_isChangedAndTheOtherAdded() throws IOException {
        var header = "Datatype Species <String>";
        try (var comparison = compare(sheet -> {
            set(sheet, 0, 0, header);
            set(sheet, 1, 0, "Dog");
            set(sheet, 2, 0, "Rabbit");
        }, sheet -> {
            set(sheet, 0, 0, header);
            set(sheet, 1, 0, "Dog");
            set(sheet, 2, 0, "Hamster");
            set(sheet, 3, 0, "Parrot");
        })) {
            // A value shares nothing with another one, yet the value replaced stands where the old one stood
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL),
                    line(1, 1, ChangeStatus.EQUAL),
                    line(2, 2, ChangeStatus.CHANGED),
                    line(LineChange.NONE, 3, ChangeStatus.ADDED)), table(comparison, header).rows());
        }
    }

    @Test
    void formulaKeptWhileACellItReadsChanged_marksTheFormulaToo() throws IOException {
        try (var comparison = compare(sheet -> withFormula(sheet, 1.0), sheet -> withFormula(sheet, 2.0))) {
            var rate = table(comparison, RATE);
            // The text of the formula of B4 is the same, its result is not
            assertEquals(List.of(new CellAddress("B3"), new CellAddress("B4")), rate.secondChanged());
        }
    }

    /** The rates, the rate of DE an Excel formula of the rate of US, computed as Excel computes it. */
    private static void withFormula(Sheet sheet, double us) {
        rates(sheet, 0, RATE, new Object[][]{{"US", us}, {"DE", 0.0}, {"FR", 1.2}, {"ES", 1.3}});
        sheet.getRow(3).getCell(1).setCellFormula("B3*1.1");
        ages(sheet, 8);
        sheet.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateAll();
    }

    @Test
    void textTurnedIntoANumber_marksTheCell() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            base(sheet);
            set(sheet, 3, 1, "1.1");
        })) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertEquals(List.of(new CellAddress("B4")), rate.secondChanged());
        }
    }

    @Test
    void commentChanged_marksTheCell() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            base(sheet);
            comment(sheet, 3, 1, "Checked by the actuary");
        })) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertEquals(List.of(new CellAddress("B4")), rate.secondChanged());
        }
    }

    /** Fills a cell with yellow, changing nothing it reads. */
    private static void fillYellow(Cell cell) {
        var style = cell.getSheet().getWorkbook().createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);
    }

    @Test
    void formattingChangedAlone_marksNoCell_butTheTable() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            base(sheet);
            fillYellow(sheet.getRow(3).getCell(1));
        })) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertTrue(rate.formattingChanged());
            assertTrue(rate.secondChanged().isEmpty());
            assertTrue(rate.rows().stream().allMatch(line -> line.status() == ChangeStatus.EQUAL));
            assertTrue(rate.formattingOnly());
            assertFalse(table(comparison, MIN_AGE).formattingChanged());
            assertFalse(table(comparison, MIN_AGE).formattingOnly());
        }
    }

    @Test
    void formattingChangedWithAValue_isNotFormattingOnly() throws IOException {
        try (var comparison = compare(XlsWorkbookComparatorTest::base, sheet -> {
            base(sheet);
            fillYellow(sheet.getRow(3).getCell(1));
            sheet.getRow(3).getCell(1).setCellValue("changed");
        })) {
            var rate = table(comparison, RATE);
            assertTrue(rate.formattingChanged());
            assertFalse(rate.secondChanged().isEmpty());
            assertFalse(rate.formattingOnly());
        }
    }

    @Test
    void mergedHeaderWidened_marksTheHeader_andAValueUnderTheMergedCellsIsNotCompared() throws IOException {
        try (var comparison = compare(sheet -> {
            base(sheet);
            sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
        }, sheet -> {
            base(sheet);
            // a value written into a cell the merged header covers is not shown, so it is not a change
            set(sheet, 0, 1, RATE);
            sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
            sheet.addMergedRegion(CellRangeAddress.valueOf("A10:B10"));
        })) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.EQUAL, rate.status());
            var minAge = table(comparison, MIN_AGE);
            assertEquals(List.of(new CellAddress("A10"), new CellAddress("B10")), minAge.secondChanged());
        }
    }

    @Test
    void headerUnmerged_marksTheHeader() throws IOException {
        try (var comparison = compare(sheet -> {
            base(sheet);
            sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
        }, XlsWorkbookComparatorTest::base)) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.CHANGED, rate.status());
            assertEquals(List.of(new CellAddress("A1"), new CellAddress("B1")), rate.firstChanged());
            assertEquals(List.of(new CellAddress("A1"), new CellAddress("B1")), rate.secondChanged());
            assertEquals(line(0, 0, ChangeStatus.CHANGED), rate.rows().getFirst());
        }
    }

    @Test
    void headerMergedOverAnAddedColumn_isTheSameHeader() throws IOException {
        try (var comparison = compare(sheet -> {
            base(sheet);
            sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
        }, sheet -> {
            set(sheet, 0, 0, RATE);
            set(sheet, 1, 0, "Country");
            set(sheet, 1, 1, "Region");
            set(sheet, 1, 2, "Rate");
            for (var i = 0; i < RATES.length; i++) {
                set(sheet, i + 2, 0, RATES[i][0]);
                set(sheet, i + 2, 1, "Any");
                set(sheet, i + 2, 2, RATES[i][1]);
            }
            sheet.addMergedRegion(CellRangeAddress.valueOf("A1:C1"));
            ages(sheet, 8);
        })) {
            var rate = table(comparison, RATE);
            assertEquals(List.of(line(0, 0, ChangeStatus.EQUAL),
                    line(LineChange.NONE, 1, ChangeStatus.ADDED),
                    line(1, 2, ChangeStatus.EQUAL)), rate.columns());
            assertTrue(rate.secondChanged().isEmpty());
        }
    }

    @Test
    void cellMergedOverAnAddedRow_isTheSameCell() throws IOException {
        try (var comparison = compare(sheet -> regions(sheet, "East", "West"),
                sheet -> regions(sheet, "East", "West", "South"))) {
            var rate = table(comparison, RATE);
            assertEquals(line(LineChange.NONE, 4, ChangeStatus.ADDED), rate.rows().get(4));
            assertTrue(rate.secondChanged().isEmpty());
            assertTrue(rate.rows().stream().noneMatch(line -> line.status() == ChangeStatus.CHANGED));
        }
    }

    @Test
    void cellLeftAloneOnceTheRowsItWasMergedWithAreRemoved_isTheSameCell() throws IOException {
        try (var comparison = compare(sheet -> regions(sheet, "East", "West", "South"),
                sheet -> regions(sheet, "South"))) {
            var rate = table(comparison, RATE);
            assertEquals(List.of(ChangeStatus.EQUAL, ChangeStatus.EQUAL, ChangeStatus.REMOVED, ChangeStatus.REMOVED,
                    ChangeStatus.EQUAL, ChangeStatus.EQUAL), rate.rows().stream().map(LineChange::status).toList());
            assertTrue(rate.firstChanged().isEmpty());
            assertTrue(rate.secondChanged().isEmpty());
        }
    }

    @Test
    void rowChangedAndRowInsertedUnderItInAMergedGroup_areChangedAndAdded_andTheOtherGroupsStayEqual()
            throws IOException {
        var costs = new String[][]{
            {"CoverageType", "Coverage"},
            {"NumberOfLives", "Number of Lives"},
            {"TotalMonthlyBenefit", "Total Monthly Benefit"},
            {"CoverageRate", "Manual Rate"}};
        var changed = new String[][]{
            {"CoverageType", "Coverage"},
            {"NumberOfLives", "Number of Lives"},
            {"TotalMonthlyBenefit", "Total Monthly Benefit"},
            {"ManualCoverageRate", "Manual Rate"},
            {"PayableCoverageRate", "Payable Rate"}};
        try (var comparison = compare(sheet -> headers(sheet, costs), sheet -> headers(sheet, changed))) {
            var table = table(comparison, HEADERS);
            assertEquals(ChangeStatus.CHANGED, table.status());
            // The changed row stays one row, and the row inserted under it is added, the group merged over both.
            assertEquals(line(5, 5, ChangeStatus.CHANGED), table.rows().get(5));
            assertEquals(line(LineChange.NONE, 6, ChangeStatus.ADDED), table.rows().get(6));
            assertEquals(1, count(table.rows(), ChangeStatus.ADDED));
            assertEquals(0, count(table.rows(), ChangeStatus.REMOVED));
            assertEquals(1, count(table.rows(), ChangeStatus.CHANGED));
            // No column is added or removed: the column of the changed cell is changed, as its row is.
            assertEquals(0, count(table.columns(), ChangeStatus.ADDED) + count(table.columns(), ChangeStatus.REMOVED));
            assertEquals(List.of("B6"), table.firstChanged().stream().map(CellAddress::formatAsString).toList());
            assertEquals(List.of("B6"), table.secondChanged().stream().map(CellAddress::formatAsString).toList());
        }
    }

    /**
     * A table of the headers of other tables: the header merged over its columns, the titles, then the rows of each
     * table, its id merged over them. The costs come first, then two more tables of two rows each.
     */
    private static void headers(Sheet sheet, String[][] costs) {
        set(sheet, 0, 0, HEADERS);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        set(sheet, 1, 0, "Table ID");
        set(sheet, 1, 1, "Id");
        set(sheet, 1, 2, "Label");
        set(sheet, 1, 3, "RendeLabel Name");
        var row = group(sheet, 2, "CostSummaryTable", costs);
        row = group(sheet, row, "ExpensesTable", new String[][]{{"Tax", "Tax"}, {"Profit", "Profit"}});
        group(sheet, row, "DemographicGenderTable", new String[][]{{"AgeBand", "Age"}, {"MonthlySalaryTotal", "Total"}});
    }

    /** The rows of one table, its id merged over them; answers the row after them. */
    private static int group(Sheet sheet, int row, String id, String[][] rows) {
        set(sheet, row, 0, id);
        for (var i = 0; i < rows.length; i++) {
            set(sheet, row + i, 1, rows[i][0]);
            set(sheet, row + i, 2, rows[i][1]);
            set(sheet, row + i, 3, "Currency");
        }
        sheet.addMergedRegion(new CellRangeAddress(row, row + rows.length - 1, 0, 0));
        return row + rows.length;
    }

    /** The rates of the regions of the US, the country merged over them, then the rate of DE. */
    private static void regions(Sheet sheet, String... regions) {
        set(sheet, 0, 0, RATE);
        set(sheet, 1, 0, "Country");
        set(sheet, 1, 1, "Region");
        set(sheet, 1, 2, "Rate");
        set(sheet, 2, 0, "US");
        for (var i = 0; i < regions.length; i++) {
            set(sheet, i + 2, 1, regions[i]);
            set(sheet, i + 2, 2, 1.0);
        }
        if (regions.length > 1) {
            sheet.addMergedRegion(new CellRangeAddress(2, regions.length + 1, 0, 0));
        }
        set(sheet, regions.length + 2, 0, "DE");
        set(sheet, regions.length + 2, 1, "Any");
        set(sheet, regions.length + 2, 2, 1.1);
    }

    @Test
    void sheetsAddedAndRemoved_holdTheirTablesAddedAndRemoved() throws IOException {
        try (var comparison = compareWorkbooks(workbook -> base(workbook.createSheet(RULES)),
                workbook -> base(workbook.createSheet("Rates")))) {
            var sheets = comparison.getSheets();
            assertEquals(List.of(RULES, "Rates"), sheets.stream().map(SheetComparison::name).toList());
            assertEquals(ChangeStatus.REMOVED, sheets.getFirst().status());
            assertEquals(List.of(ChangeStatus.REMOVED, ChangeStatus.REMOVED),
                    sheets.getFirst().tables().stream().map(TableComparison::status).toList());
            assertEquals(ChangeStatus.ADDED, sheets.get(1).status());
            var added = sheets.get(1).tables().getFirst();
            assertNull(added.first());
            assertEquals(CellRangeAddress.valueOf("A1:B6"), added.second());
            assertTrue(added.rows().isEmpty());
            assertNotNull(comparison.firstSheet(RULES));
            assertNull(comparison.secondSheet(RULES));
            assertNotNull(comparison.secondSheet("Rates"));
        }
    }

    @Test
    void notAWorkbook_isNotCompared() throws IOException {
        var text = Files.writeString(folder.resolve("text.xlsx"), "not a workbook").toFile();
        var workbook = write("book.xlsx", book -> base(book.createSheet(RULES)));
        assertThrows(IOException.class, () -> XlsWorkbookComparator.compare(workbook, text));
        assertThrows(IOException.class, () -> XlsWorkbookComparator.compare(text, workbook));
    }

    /** The rates at A1:B6 and the minimal ages at A9:B12. */
    private static void base(Sheet sheet) {
        rates(sheet, 0, RATE, RATES);
        ages(sheet, 8);
    }

    private static void ages(Sheet sheet, int row) {
        ages(sheet, row, 0);
    }

    private static void ages(Sheet sheet, int row, int column) {
        set(sheet, row, column, MIN_AGE);
        set(sheet, row + 1, column, "Country");
        set(sheet, row + 1, column + 1, "Min Age");
        for (var i = 0; i < AGES.length; i++) {
            set(sheet, row + i + 2, column, AGES[i][0]);
            set(sheet, row + i + 2, column + 1, AGES[i][1]);
        }
    }

    private static void rates(Sheet sheet, int row, String header, Object[][] rates) {
        set(sheet, row, 0, header);
        set(sheet, row + 1, 0, "Country");
        set(sheet, row + 1, 1, "Rate");
        for (var i = 0; i < rates.length; i++) {
            set(sheet, row + i + 2, 0, rates[i][0]);
            set(sheet, row + i + 2, 1, rates[i][1]);
        }
    }

    private static void set(Sheet sheet, int row, int column, Object value) {
        var cells = sheet.getRow(row) == null ? sheet.createRow(row) : sheet.getRow(row);
        var cell = cells.createCell(column);
        if (value instanceof Double number) {
            cell.setCellValue(number);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private static void comment(Sheet sheet, int row, int column, String text) {
        var helper = sheet.getWorkbook().getCreationHelper();
        var anchor = helper.createClientAnchor();
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        anchor.setCol1(column);
        anchor.setRow1(row);
        anchor.setCol2(column + 2);
        anchor.setRow2(row + 2);
        var comment = sheet.createDrawingPatriarch().createCellComment(anchor);
        comment.setString(helper.createRichTextString(text));
        sheet.getRow(row).getCell(column).setCellComment(comment);
    }

    private static long count(List<LineChange> lines, ChangeStatus status) {
        return lines.stream().filter(line -> line.status() == status).count();
    }

    private static LineChange line(int first, int second, ChangeStatus status) {
        return new LineChange(first, second, status);
    }

    private static SheetComparison sheet(WorkbookComparison comparison) {
        assertEquals(1, comparison.getSheets().size());
        return comparison.getSheets().getFirst();
    }

    private static TableComparison table(WorkbookComparison comparison, String name) {
        return sheet(comparison).tables()
                .stream()
                .filter(table -> table.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table " + name));
    }

    private WorkbookComparison compare(Consumer<Sheet> first, Consumer<Sheet> second) throws IOException {
        return compareWorkbooks(workbook -> first.accept(workbook.createSheet(RULES)),
                workbook -> second.accept(workbook.createSheet(RULES)));
    }

    private WorkbookComparison compareWorkbooks(Consumer<Workbook> first, Consumer<Workbook> second)
            throws IOException {
        return XlsWorkbookComparator.compare(write("first.xlsx", first), write("second.xlsx", second));
    }

    private File write(String name, Consumer<Workbook> content) throws IOException {
        var file = folder.resolve(name);
        try (var workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            content.accept(workbook);
            workbook.write(out);
        }
        return file.toFile();
    }
}
