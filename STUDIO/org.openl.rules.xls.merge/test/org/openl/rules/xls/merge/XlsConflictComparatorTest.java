package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.xls.merge.compare.ChangeStatus;
import org.openl.rules.xls.merge.compare.ConflictComparison;
import org.openl.rules.xls.merge.compare.ConflictLine;
import org.openl.rules.xls.merge.compare.ConflictTable;
import org.openl.rules.xls.merge.compare.LineChange;

/**
 * Covers the comparison of the base of a workbook with the two versions of a conflict: the tables joined by the
 * base, the lines lined up by the lines of the base, and the cells both versions changed differently.
 */
class XlsConflictComparatorTest {

    private static final String RULES = "Rules";
    private static final String RATE = "SmartRules Double rate(String country)";
    private static final String AGE = "SmartRules Integer minAge(String country)";
    private static final int NONE = LineChange.NONE;
    private static final XlsConflictComparator.LinesAlike NOTHING_ALIKE = (yours, theirs) -> false;

    @TempDir
    Path folder;

    @Test
    void linesInsertedByBothVersionsStandBeforeTheLineOfTheBase_yoursFirst() {
        var yours = List.of(line(0, 0, ChangeStatus.EQUAL), line(NONE, 1, ChangeStatus.ADDED), line(1, 2, ChangeStatus.EQUAL));
        var theirs = List.of(line(0, 0, ChangeStatus.EQUAL), line(NONE, 1, ChangeStatus.ADDED), line(1, 2, ChangeStatus.CHANGED));

        assertEquals(List.of(
                new ConflictLine(0, 0, 0, ChangeStatus.EQUAL, ChangeStatus.EQUAL),
                new ConflictLine(NONE, 1, NONE, ChangeStatus.ADDED, ChangeStatus.EQUAL),
                new ConflictLine(NONE, NONE, 1, ChangeStatus.EQUAL, ChangeStatus.ADDED),
                new ConflictLine(1, 2, 2, ChangeStatus.EQUAL, ChangeStatus.CHANGED)), XlsConflictComparator.lines(yours, theirs, NOTHING_ALIKE));
    }

    @Test
    void linesBothVersionsInsertedAlikeAtOnePlaceAreOneLine_theOthersStandAroundIt() {
        var yours = List.of(line(0, 0, ChangeStatus.EQUAL), line(NONE, 1, ChangeStatus.ADDED),
                line(NONE, 2, ChangeStatus.ADDED), line(1, 3, ChangeStatus.EQUAL));
        var theirs = List.of(line(0, 0, ChangeStatus.EQUAL), line(NONE, 1, ChangeStatus.ADDED),
                line(NONE, 2, ChangeStatus.ADDED), line(1, 3, ChangeStatus.EQUAL));
        // Your line 1 is their line 2: their line 1 is theirs alone, your line 2 yours alone
        XlsConflictComparator.LinesAlike alike = (your, their) -> your == 1 && their == 2;

        assertEquals(List.of(
                new ConflictLine(0, 0, 0, ChangeStatus.EQUAL, ChangeStatus.EQUAL),
                new ConflictLine(NONE, NONE, 1, ChangeStatus.EQUAL, ChangeStatus.ADDED),
                new ConflictLine(NONE, 1, 2, ChangeStatus.ADDED, ChangeStatus.ADDED),
                new ConflictLine(NONE, 2, NONE, ChangeStatus.ADDED, ChangeStatus.EQUAL),
                new ConflictLine(1, 3, 3, ChangeStatus.EQUAL, ChangeStatus.EQUAL)),
                XlsConflictComparator.lines(yours, theirs, alike));
    }

    @Test
    void aRowBothVersionsInsertedAlikeIsOneRowOfTheTable() throws IOException {
        try (var comparison = compare(sheet -> rates(sheet, "1.0", "1.1", "1.2"),
                XlsConflictComparatorTest::ratesWithIt,
                XlsConflictComparatorTest::ratesWithIt)) {
            var rate = table(comparison, RATE);

            // Both put IT under US: one row, added by both
            assertEquals(new ConflictLine(NONE, 3, 3, ChangeStatus.ADDED, ChangeStatus.ADDED), rate.rows().get(3));
            assertEquals(6, rate.rows().size());
        }
    }

    /** The rates of three countries with IT added under US. */
    private static void ratesWithIt(Sheet sheet) {
        set(sheet, 0, 0, RATE);
        set(sheet, 1, 0, "Country");
        set(sheet, 1, 1, "Rate");
        var rates = new String[][]{{"US", "1.0"}, {"IT", "1.4"}, {"DE", "1.1"}, {"FR", "1.2"}};
        for (var i = 0; i < rates.length; i++) {
            set(sheet, i + 2, 0, rates[i][0]);
            set(sheet, i + 2, 1, rates[i][1]);
        }
    }

    @Test
    void aLineOneVersionRemovedStandsAgainstTheLineTheOtherKeptAndTheLinesAddedAtTheEndEndTheTable() {
        var yours = List.of(line(0, 0, ChangeStatus.EQUAL), line(1, NONE, ChangeStatus.REMOVED));
        var theirs = List.of(line(0, 0, ChangeStatus.EQUAL), line(1, 1, ChangeStatus.EQUAL), line(NONE, 2, ChangeStatus.ADDED));

        assertEquals(List.of(
                new ConflictLine(0, 0, 0, ChangeStatus.EQUAL, ChangeStatus.EQUAL),
                new ConflictLine(1, NONE, 1, ChangeStatus.REMOVED, ChangeStatus.EQUAL),
                new ConflictLine(NONE, NONE, 2, ChangeStatus.EQUAL, ChangeStatus.ADDED)), XlsConflictComparator.lines(yours, theirs, NOTHING_ALIKE));
    }

    @Test
    void aCellBothVersionsChangedDifferentlyIsAConflict_andOneTheyChangedAlikeIsNot() throws IOException {
        try (var comparison = compare(sheet -> rates(sheet, "1.0", "1.1", "1.2"),
                sheet -> rates(sheet, "2.0", "1.5", "1.2"),
                sheet -> rates(sheet, "2.0", "1.6", "1.2"))) {
            var table = table(comparison, RATE);

            assertEquals(ChangeStatus.CHANGED, table.yoursStatus());
            assertEquals(ChangeStatus.CHANGED, table.theirsStatus());
            // US was set to 2.0 by both, DE to 1.5 by one and to 1.6 by the other.
            assertEquals(List.of(address("B3"), address("B4")), table.yoursChanged());
            assertEquals(List.of(address("B4")), table.yoursConflicts());
            assertEquals(List.of(address("B4")), table.theirsConflicts());
        }
    }

    @Test
    void theTablesAreJoinedByTheBase_aTableTheirVersionAddedStandsAlone() throws IOException {
        try (var comparison = compare(sheet -> rates(sheet, "1.0", "1.1", "1.2"),
                sheet -> rates(sheet, "1.0", "1.1", "1.2"),
                sheet -> {
                    rates(sheet, "1.0", "1.1", "1.2");
                    ages(sheet, 8);
                })) {
            var rate = table(comparison, RATE);
            assertEquals(ChangeStatus.EQUAL, rate.yoursStatus());
            assertEquals(ChangeStatus.EQUAL, rate.theirsStatus());
            assertEquals(5, rate.rows().size());

            var age = table(comparison, AGE);
            assertNull(age.base());
            assertNull(age.yours());
            assertEquals(ChangeStatus.EQUAL, age.yoursStatus());
            assertEquals(ChangeStatus.ADDED, age.theirsStatus());
            assertEquals(List.of(), age.rows());
            assertEquals(ChangeStatus.CHANGED, comparison.getSheets().getFirst().theirsStatus());
        }
    }

    @Test
    void aTableYourVersionRemovedHoldsNoneOfTheLinesOfTheBase() throws IOException {
        try (var comparison = compare(sheet -> rates(sheet, "1.0", "1.1", "1.2"),
                // Another table elsewhere: a table at the place of the rates would be the rates renamed
                sheet -> ages(sheet, 10),
                sheet -> rates(sheet, "1.0", "1.5", "1.2"))) {
            var rate = table(comparison, RATE);

            assertEquals(ChangeStatus.REMOVED, rate.yoursStatus());
            assertEquals(ChangeStatus.CHANGED, rate.theirsStatus());
            assertEquals(new ConflictLine(3, NONE, 3, ChangeStatus.REMOVED, ChangeStatus.CHANGED), rate.rows().get(3));
            assertEquals(List.of(), rate.yoursConflicts());
        }
    }

    @Test
    void aTableYourVersionRemovedIsNamedAsTheirVersionNamesIt() throws IOException {
        var renamed = "SmartRules Double newRate(String country)";
        try (var comparison = compare(sheet -> rates(sheet, "1.0", "1.1", "1.2"),
                sheet -> ages(sheet, 10),
                sheet -> {
                    rates(sheet, "1.0", "1.1", "1.2");
                    set(sheet, 0, 0, renamed);
                })) {
            var rate = table(comparison, renamed);

            assertEquals(ChangeStatus.REMOVED, rate.yoursStatus());
            assertEquals(ChangeStatus.CHANGED, rate.theirsStatus());
        }
    }

    @Test
    void theThreeVersionsAreReadFromTheComparison() throws IOException {
        try (var comparison = compare(sheet -> rates(sheet, "1.0", "1.1", "1.2"),
                sheet -> rates(sheet, "2.0", "1.1", "1.2"),
                sheet -> rates(sheet, "3.0", "1.1", "1.2"))) {
            assertEquals("1.0", comparison.baseSheet(RULES).getRow(2).getCell(1).getStringCellValue());
            assertEquals("2.0", comparison.yoursSheet(RULES).getRow(2).getCell(1).getStringCellValue());
            assertEquals("3.0", comparison.theirsSheet(RULES).getRow(2).getCell(1).getStringCellValue());
        }
    }

    @Test
    void aVersionThatIsNoWorkbookIsNotCompared() throws IOException {
        var base = write("base.xlsx", sheet -> rates(sheet, "1.0", "1.1", "1.2"));
        var text = Files.writeString(folder.resolve("text.xlsx"), "not a workbook").toFile();

        assertThrows(IOException.class, () -> XlsConflictComparator.compare(base, base, text));
    }

    /** The rates of three countries at A1, the rate of each a text. */
    private static void rates(Sheet sheet, String us, String de, String fr) {
        set(sheet, 0, 0, RATE);
        set(sheet, 1, 0, "Country");
        set(sheet, 1, 1, "Rate");
        var rates = new String[][]{{"US", us}, {"DE", de}, {"FR", fr}};
        for (var i = 0; i < rates.length; i++) {
            set(sheet, i + 2, 0, rates[i][0]);
            set(sheet, i + 2, 1, rates[i][1]);
        }
    }

    private static void ages(Sheet sheet, int row) {
        set(sheet, row, 0, AGE);
        set(sheet, row + 1, 0, "Country");
        set(sheet, row + 1, 1, "Min Age");
        set(sheet, row + 2, 0, "US");
        set(sheet, row + 2, 1, "16");
    }

    private static void set(Sheet sheet, int row, int column, String value) {
        var cells = sheet.getRow(row) == null ? sheet.createRow(row) : sheet.getRow(row);
        cells.createCell(column).setCellValue(value);
    }

    private static LineChange line(int first, int second, ChangeStatus status) {
        return new LineChange(first, second, status);
    }

    private static CellAddress address(String address) {
        return new CellAddress(address);
    }

    private static ConflictTable table(ConflictComparison comparison, String name) {
        return comparison.getSheets()
                .getFirst()
                .tables()
                .stream()
                .filter(table -> table.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table " + name));
    }

    private ConflictComparison compare(Consumer<Sheet> base, Consumer<Sheet> yours, Consumer<Sheet> theirs)
            throws IOException {
        return XlsConflictComparator.compare(write("base.xlsx", base), write("yours.xlsx", yours),
                write("theirs.xlsx", theirs));
    }

    private File write(String name, Consumer<Sheet> content) throws IOException {
        var file = folder.resolve(name);
        try (var workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            content.accept(workbook.createSheet(RULES));
            workbook.write(out);
        }
        return file.toFile();
    }
}
