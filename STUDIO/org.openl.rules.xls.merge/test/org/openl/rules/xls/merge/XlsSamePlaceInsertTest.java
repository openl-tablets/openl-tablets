package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.xls.merge.MergeFixture.RULES;
import static org.openl.rules.xls.merge.MergeFixture.bold;
import static org.openl.rules.xls.merge.MergeFixture.cell;
import static org.openl.rules.xls.merge.MergeFixture.cellOf;
import static org.openl.rules.xls.merge.MergeFixture.insertLine;
import static org.openl.rules.xls.merge.MergeFixture.isBold;
import static org.openl.rules.xls.merge.MergeFixture.move;
import static org.openl.rules.xls.merge.MergeFixture.number;
import static org.openl.rules.xls.merge.MergeFixture.regionIndex;
import static org.openl.rules.xls.merge.MergeFixture.set;
import static org.openl.rules.xls.merge.MergeFixture.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.TableConflictReason;

/**
 * Covers the lines both revisions inserted at the same place of a table: all of them are kept, those of OUR revision
 * first, and a line both revisions inserted alike is kept once.
 */
class XlsSamePlaceInsertTest {

    /** The rules of the countries at A1:B5 and the data of the drivers at E1:G4, each with a merged header. */
    private static final MergeFixture FIXTURE = new MergeFixture(XlsSamePlaceInsertTest::rulesAndDrivers);
    private static final String RATES = RULES + "/A1:B5";
    private static final String DRIVERS = RULES + "/E1:G4";
    /** The first column of the drivers. */
    private static final int DRIVERS_COLUMN = 4;
    private static final List<String> COUNTRIES = List.of("US", "CA", "MX");
    private static final List<String> FIELDS = List.of("name", "age", "note");
    private static final Map<String, Integer> RATES_OF = Map.of("US", 1,
            "CA", 2,
            "MX", 3,
            "UK", 4,
            "IE", 5,
            "DE", 6,
            "AT", 7,
            "FR", 8);
    private static final IndexedColors OUR_COLOR = IndexedColors.YELLOW;
    private static final IndexedColors THEIR_COLOR = IndexedColors.LIGHT_GREEN;

    /** What the revisions insert into a table. */
    enum Lines {
        ROWS,
        COLUMNS
    }

    /** Where the revisions insert the lines: before the first line of values, between two of them, or after all. */
    enum Place {
        BEGINNING,
        MIDDLE,
        END
    }

    /** How the inserted lines look: as the lines around them, filled with a color, or with their values merged. */
    enum Look {
        PLAIN,
        STYLED,
        MERGED
    }

    static Stream<Arguments> matrix() {
        return Arrays.stream(Lines.values())
                .flatMap(lines -> Arrays.stream(Place.values())
                        .flatMap(place -> Arrays.stream(Look.values()).map(look -> Arguments.of(lines, place, look))));
    }

    @ParameterizedTest(name = "{0} at the {1}, {2}")
    @MethodSource("matrix")
    void linesInsertedAtTheSamePlace_areAllKeptOursFirst(Lines lines, Place place, Look look) throws IOException {
        if (lines == Lines.ROWS) {
            var row = switch (place) {
                case BEGINNING -> 2;
                case MIDDLE -> 3;
                case END -> 5;
            };
            var merged = FIXTURE.mergeXlsx(insertCountries(row, look, OUR_COLOR, "UK", "IE"),
                    insertCountries(row, look, THEIR_COLOR, "DE", "AT"));
            assertRows(merged, row, look);
        } else if (place == Place.BEGINNING) {
            // The first column of a table holds its header, so the header of each revision would stand in a row.
            var tables = FIXTURE.conflicts(XSSFWorkbook::new,
                    insertFields(0, look, OUR_COLOR, "zip", "city"),
                    insertFields(0, look, THEIR_COLOR, "phone", "email")).sheets().getFirst().tables();
            assertEquals(TableConflictReason.ROWS, drivers(tables).reason());
        } else {
            var index = place == Place.MIDDLE ? 1 : 3;
            var merged = FIXTURE.mergeXlsx(insertFields(index, look, OUR_COLOR, "zip", "city"),
                    insertFields(index, look, THEIR_COLOR, "phone", "email"));
            assertColumns(merged, index, look);
        }
    }

    private static void assertRows(Sheet merged, int row, Look look) {
        var countries = new ArrayList<>(COUNTRIES);
        countries.addAll(row - 2, List.of("UK", "IE", "DE", "AT"));
        assertCountries(merged, countries.toArray(String[]::new));
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("A1:B1")));
        for (var i = 0; i < countries.size(); i++) {
            var country = countries.get(i);
            var inserted = i >= row - 2 && i < row + 2;
            var second = i == row - 1 || i == row + 1;
            if (look != Look.MERGED || !second) {
                assertEquals((double) RATES_OF.get(country), number(merged, 2 + i, 1), country);
            }
            var color = i < row ? OUR_COLOR : THEIR_COLOR;
            assertFill(merged, 2 + i, 0, look == Look.STYLED && inserted ? color : null);
        }
        if (look == Look.MERGED) {
            assertTrue(merged.getMergedRegions().contains(new CellRangeAddress(row, row + 1, 1, 1)), "UK and IE");
            assertTrue(merged.getMergedRegions().contains(new CellRangeAddress(row + 2, row + 3, 1, 1)), "DE, AT");
        }
    }

    private static void assertColumns(Sheet merged, int index, Look look) {
        var fields = new ArrayList<>(FIELDS);
        fields.addAll(index, List.of("zip", "city", "phone", "email"));
        for (var i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            var column = DRIVERS_COLUMN + i;
            assertEquals(field, text(merged, 1, column));
            var inserted = i >= index && i < index + 4;
            if (inserted) {
                assertEquals(field + 1, text(merged, 2, column));
                assertEquals(look == Look.MERGED ? CellType.BLANK : CellType.STRING, typeOf(merged, 3, column));
                var color = i < index + 2 ? OUR_COLOR : THEIR_COLOR;
                assertFill(merged, 1, column, look == Look.STYLED ? color : null);
                assertEquals(look == Look.MERGED,
                        merged.getMergedRegions().contains(new CellRangeAddress(2, 3, column, column)), field);
            } else {
                assertFill(merged, 1, column, null);
            }
        }
        assertEquals("Ann", text(merged, 2, DRIVERS_COLUMN));
        assertNoField(merged, DRIVERS_COLUMN + fields.size());
        assertTrue(merged.getMergedRegions()
                        .contains(new CellRangeAddress(0, 0, DRIVERS_COLUMN, DRIVERS_COLUMN + fields.size() - 1)),
                "The header stretches over the fields of both branches");
    }

    @Test
    void sameRowInsertedInBothAmongOthers_isKeptOnce() throws IOException {
        var merged = FIXTURE.mergeXlsx(insertCountries(5, Look.PLAIN, OUR_COLOR, "UK", "FR"),
                insertCountries(5, Look.PLAIN, THEIR_COLOR, "FR", "DE"));

        assertCountries(merged, "US", "CA", "MX", "UK", "FR", "DE");
    }

    @Test
    void sameRowsInsertedInBothInAnotherOrder_keepTheOrderOfOurs() throws IOException {
        var merged = FIXTURE.mergeXlsx(insertCountries(5, Look.PLAIN, OUR_COLOR, "UK", "FR"),
                insertCountries(5, Look.PLAIN, THEIR_COLOR, "FR", "UK"));

        assertCountries(merged, "US", "CA", "MX", "UK", "FR");
    }

    @Test
    void sameRowInsertedInBothWithOtherFormatting_keepsOurs() throws IOException {
        var merged = FIXTURE.mergeXlsx(insertCountries(3, Look.PLAIN, OUR_COLOR, "UK", "IE")
                        .andThen(sheet -> bold(sheet, 3, 0)),
                insertCountries(3, Look.STYLED, THEIR_COLOR, "UK", "DE"));

        assertCountries(merged, "US", "UK", "IE", "DE", "CA", "MX");
        assertTrue(isBold(merged, 3, 0));
        assertFill(merged, 3, 0, null);
        assertFill(merged, 5, 0, THEIR_COLOR);
    }

    @Test
    void sameRowInsertedInBothWithAnotherComment_isKeptTwice() throws IOException {
        // A comment is content, as a value is: the rows differ, so the row of each branch is kept.
        var merged = FIXTURE.mergeXlsx(insertCountries(3, Look.PLAIN, OUR_COLOR, "UK")
                        .andThen(sheet -> MergeFixture.comment(sheet, 3, 1, "D7:F9", "estimated")),
                insertCountries(3, Look.PLAIN, THEIR_COLOR, "UK"));

        assertCountries(merged, "US", "UK", "UK", "CA", "MX");
        assertEquals("estimated", cell(merged, 3, 1).getCellComment().getString().getString());
        assertNull(cell(merged, 4, 1).getCellComment());
    }

    @Test
    void theSameBranches_mergeInTheSameOrderEveryTime() throws IOException {
        var our = insertCountries(3, Look.PLAIN, OUR_COLOR, "UK", "IE");
        var their = insertCountries(3, Look.PLAIN, THEIR_COLOR, "DE", "AT");

        var first = countriesOf(FIXTURE.mergeXlsx(our, their));
        assertEquals(List.of("US", "UK", "IE", "DE", "AT", "CA", "MX"), first);
        for (var i = 0; i < 3; i++) {
            assertEquals(first, countriesOf(FIXTURE.mergeXlsx(our, their)));
        }
    }

    @Test
    void mergedCellSpanningThePlace_holdsTheRowsOfBoth() throws IOException {
        // CA and MX share their rate; each branch inserts a country between them, under the same rate.
        var fixture = new MergeFixture(workbook -> {
            rulesAndDrivers(workbook);
            var sheet = workbook.getSheet(RULES);
            sheet.getRow(4).removeCell(cell(sheet, 4, 1));
            sheet.addMergedRegion(CellRangeAddress.valueOf("B4:B5"));
        });
        var merged = fixture.mergeXlsx(sheet -> insertUnderSharedRate(sheet, "UK"),
                sheet -> insertUnderSharedRate(sheet, "DE"));

        assertCountries(merged, "US", "CA", "UK", "DE", "MX");
        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("B4:B7")));
        assertEquals(2, number(merged, 3, 1));
    }

    /**
     * Inserts a country between CA and MX, whose shared rate stretches over it.
     */
    private static void insertUnderSharedRate(Sheet sheet, String country) {
        insertLine(sheet, 4, 0);
        set(sheet, 4, 0, country);
        sheet.removeMergedRegion(regionIndex(sheet, "B4:B5"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("B4:B6"));
    }

    @Test
    void headerStretchedInOneBranchOnly_coversTheFieldsOfBoth() throws IOException {
        Consumer<Sheet> stretched = insertFields(3, Look.PLAIN, OUR_COLOR, "zip");
        Consumer<Sheet> kept = sheet -> {
            set(sheet, 1, 7, "phone");
            set(sheet, 2, 7, "phone1");
            set(sheet, 3, 7, "phone2");
        };

        var header = CellRangeAddress.valueOf("E1:I1");
        assertTrue(FIXTURE.mergeXlsx(stretched, kept).getMergedRegions().contains(header), "Ours stretched");
        assertTrue(FIXTURE.mergeXlsx(kept, stretched).getMergedRegions().contains(header), "Theirs stretched");
    }

    @Test
    void headerOverAValueOfTheOtherBranch_isConflict() throws IOException {
        var tables = FIXTURE.conflicts(XSSFWorkbook::new, insertFields(3, Look.PLAIN, OUR_COLOR, "zip"), sheet -> {
            set(sheet, 0, 7, "Drivers of the fleet");
            set(sheet, 1, 7, "phone");
        }).sheets().getFirst().tables();

        assertEquals(TableConflictReason.MERGED_CELLS, drivers(tables).reason());
    }

    @Test
    void headerStretchedOverSomeOfTheFields_isConflict() throws IOException {
        Consumer<Sheet> our = insertFields(3, Look.PLAIN, OUR_COLOR, "zip", "city").andThen(sheet -> {
            sheet.removeMergedRegion(regionIndex(sheet, "E1:I1"));
            sheet.addMergedRegion(CellRangeAddress.valueOf("E1:H1"));
        });

        var tables = FIXTURE.conflicts(XSSFWorkbook::new, our, insertFields(3, Look.PLAIN, THEIR_COLOR, "phone"))
                .sheets()
                .getFirst()
                .tables();
        assertEquals(TableConflictReason.MERGED_CELLS, drivers(tables).reason());
    }

    @Test
    void mergedCellsOfARowBothInserted_areKeptOnce() throws IOException {
        var merged = FIXTURE.mergeXlsx(insertNote().andThen(insertCountries(6, Look.PLAIN, OUR_COLOR, "UK")),
                insertNote().andThen(insertCountries(6, Look.PLAIN, THEIR_COLOR, "DE")));

        assertCountries(merged, "US", "CA", "MX", "Asia", "UK", "DE");
        assertEquals(1, merged.getMergedRegions()
                .stream()
                .filter(CellRangeAddress.valueOf("A6:B6")::equals)
                .count());
    }

    @Test
    void mergedCellsOfARowBothInsertedThatDiffer_isConflict() throws IOException {
        // Their branch merges the label of the row into the row it inserts after it.
        Consumer<Sheet> their = sheet -> {
            set(sheet, 5, 0, "Asia");
            set(sheet, 6, 1, 9);
            sheet.addMergedRegion(CellRangeAddress.valueOf("A6:A7"));
        };

        var tables = FIXTURE.conflicts(XSSFWorkbook::new, insertNote(), their).sheets().getFirst().tables();
        assertEquals(TableConflictReason.MERGED_CELLS, rates(tables).reason());
    }

    @Test
    void sameRowWithMergedCellsInsertedInBoth_isKeptOnce() throws IOException {
        // Both branches insert the same label merged over its row; their branch changes the rate of MX as well.
        Consumer<Sheet> asia = sheet -> {
            insertLine(sheet, 3, 0);
            set(sheet, 3, 0, "Asia");
            sheet.addMergedRegion(CellRangeAddress.valueOf("A4:B4"));
        };
        var merged = FIXTURE.mergeXlsx(asia, asia.andThen(sheet -> set(sheet, 5, 1, 30)));

        assertCountries(merged, "US", "Asia", "CA", "MX");
        assertEquals(30, number(merged, 5, 1));
        assertEquals(1, merged.getMergedRegions()
                .stream()
                .filter(CellRangeAddress.valueOf("A4:B4")::equals)
                .count());
    }

    @Test
    void propertiesAddedInBothToATableWithoutThem_isConflict() throws IOException {
        // Each branch would start the properties of the drivers in a row of its own.
        var tables = FIXTURE.conflicts(XSSFWorkbook::new,
                sheet -> insertProperty(sheet, 1, "category", "Auto"),
                sheet -> insertProperty(sheet, 1, "state", "NY")).sheets().getFirst().tables();

        assertEquals(TableConflictReason.ROWS, drivers(tables).reason());
    }

    @Test
    void propertiesAddedInBothAfterTheProperties_areMergedUnderOneCell() throws IOException {
        // The drivers have a property already, and each branch adds one after it, stretching the properties cell.
        var fixture = new MergeFixture(workbook -> {
            rulesAndDrivers(workbook);
            insertProperty(workbook.getSheet(RULES), 1, "category", "Auto");
        });
        var merged = fixture.mergeXlsx(sheet -> addProperty(sheet, "state", "NY"),
                sheet -> addProperty(sheet, "lob", "Personal"));

        assertTrue(merged.getMergedRegions().contains(CellRangeAddress.valueOf("E2:E4")));
        assertEquals("properties", text(merged, 1, DRIVERS_COLUMN));
        assertEquals(List.of("category", "state", "lob"),
                IntStream.rangeClosed(1, 3).mapToObj(row -> text(merged, row, DRIVERS_COLUMN + 1)).toList());
        assertEquals("name", text(merged, 4, DRIVERS_COLUMN));
    }

    /**
     * Inserts a row of properties into the drivers at a row, under the header: the rows below it move down.
     */
    private static void insertProperty(Sheet sheet, int row, String name, String value) {
        var last = 1;
        while (cell(sheet, last + 1, DRIVERS_COLUMN) != null) {
            last++;
        }
        move(sheet, new CellRangeAddress(row, last, DRIVERS_COLUMN, DRIVERS_COLUMN + 2).formatAsString(), 1, 0);
        set(sheet, row, DRIVERS_COLUMN, "properties");
        set(sheet, row, DRIVERS_COLUMN + 1, name);
        set(sheet, row, DRIVERS_COLUMN + 2, value);
    }

    /**
     * Adds a property after the one the drivers have, the way OpenL Studio does: the properties cell stretches over it.
     */
    private static void addProperty(Sheet sheet, String name, String value) {
        insertProperty(sheet, 2, name, value);
        cell(sheet, 2, DRIVERS_COLUMN).setBlank();
        sheet.addMergedRegion(CellRangeAddress.valueOf("E2:E3"));
    }

    /**
     * Inserts a label after the countries, merged over the row.
     */
    private static Consumer<Sheet> insertNote() {
        return sheet -> {
            set(sheet, 5, 0, "Asia");
            sheet.addMergedRegion(CellRangeAddress.valueOf("A6:B6"));
        };
    }

    /**
     * Inserts countries into the rules from a row on, the way OpenL Studio does: the rows of the rules below move down.
     */
    private static Consumer<Sheet> insertCountries(int row, Look look, IndexedColors color, String... countries) {
        return sheet -> {
            for (var i = 0; i < countries.length; i++) {
                insertLine(sheet, row + i, 0);
                set(sheet, row + i, 0, countries[i]);
                if (look != Look.MERGED || i == 0) {
                    set(sheet, row + i, 1, RATES_OF.get(countries[i]));
                }
                if (look == Look.STYLED) {
                    fill(sheet, row + i, 0, color);
                    fill(sheet, row + i, 1, color);
                }
            }
            if (look == Look.MERGED) {
                sheet.addMergedRegion(new CellRangeAddress(row, row + countries.length - 1, 1, 1));
            }
        };
    }

    /**
     * Inserts fields into the drivers from a column on, counted from their first one: the columns after it move to
     * the right, and the merged header stretches over the new ones. A field holds its name and a value for each
     * driver.
     */
    private static Consumer<Sheet> insertFields(int index, Look look, IndexedColors color, String... fields) {
        return sheet -> {
            for (var i = 0; i < fields.length; i++) {
                var column = DRIVERS_COLUMN + index + i;
                var last = lastColumn(sheet);
                if (column <= last) {
                    move(sheet, new CellRangeAddress(1, 3, column, last).formatAsString(), 0, 1);
                }
                set(sheet, 1, column, fields[i]);
                set(sheet, 2, column, fields[i] + 1);
                if (look == Look.MERGED) {
                    sheet.addMergedRegion(new CellRangeAddress(2, 3, column, column));
                } else {
                    set(sheet, 3, column, fields[i] + 2);
                }
                if (look == Look.STYLED) {
                    IntStream.rangeClosed(1, 3).forEach(row -> fill(sheet, row, column, color));
                }
            }
            var header = CellRangeAddress.valueOf("E1:G1");
            sheet.removeMergedRegion(regionIndex(sheet, header.formatAsString()));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, DRIVERS_COLUMN, header.getLastColumn() + fields.length));
        };
    }

    /**
     * The last column of the drivers, by the names of their fields.
     */
    private static int lastColumn(Sheet sheet) {
        var column = DRIVERS_COLUMN;
        while (cell(sheet, 1, column + 1) != null) {
            column++;
        }
        return column;
    }

    private static void rulesAndDrivers(Workbook workbook) {
        var sheet = workbook.createSheet(RULES);
        set(sheet, 0, 0, "Rules String rate(String country)");
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
        set(sheet, 1, 0, "C1");
        set(sheet, 1, 1, "RET1");
        for (var i = 0; i < COUNTRIES.size(); i++) {
            set(sheet, 2 + i, 0, COUNTRIES.get(i));
            set(sheet, 2 + i, 1, RATES_OF.get(COUNTRIES.get(i)));
        }
        set(sheet, 0, DRIVERS_COLUMN, "Data Driver drivers");
        sheet.addMergedRegion(CellRangeAddress.valueOf("E1:G1"));
        for (var i = 0; i < FIELDS.size(); i++) {
            set(sheet, 1, DRIVERS_COLUMN + i, FIELDS.get(i));
        }
        set(sheet, 2, DRIVERS_COLUMN, "Ann");
        set(sheet, 2, DRIVERS_COLUMN + 1, 30);
        set(sheet, 2, DRIVERS_COLUMN + 2, "a");
        set(sheet, 3, DRIVERS_COLUMN, "Bob");
        set(sheet, 3, DRIVERS_COLUMN + 1, 40);
        set(sheet, 3, DRIVERS_COLUMN + 2, "b");
    }

    private static void fill(Sheet sheet, int row, int column, IndexedColors color) {
        var style = sheet.getWorkbook().createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellOf(sheet, row, column).setCellStyle(style);
    }

    private static void assertFill(Sheet sheet, int row, int column, IndexedColors color) {
        var style = cell(sheet, row, column).getCellStyle();
        if (color == null) {
            assertEquals(FillPatternType.NO_FILL, style.getFillPattern());
        } else {
            assertEquals(FillPatternType.SOLID_FOREGROUND, style.getFillPattern());
            assertEquals(color.getIndex(), style.getFillForegroundColor());
        }
    }

    /**
     * Checks the countries of the rules from their first row of values, and that the rules end after them.
     */
    private static void assertCountries(Sheet sheet, String... countries) {
        assertEquals(List.of(countries), countriesOf(sheet));
    }

    private static List<String> countriesOf(Sheet sheet) {
        var countries = new ArrayList<String>();
        for (var row = 2; cell(sheet, row, 0) != null && cell(sheet, row, 0).getCellType() != CellType.BLANK; row++) {
            countries.add(text(sheet, row, 0));
        }
        return countries;
    }

    private static void assertNoField(Sheet sheet, int column) {
        var field = cell(sheet, 1, column);
        assertTrue(field == null || field.getCellType() == CellType.BLANK);
    }

    private static CellType typeOf(Sheet sheet, int row, int column) {
        var cell = cell(sheet, row, column);
        return cell == null ? CellType.BLANK : cell.getCellType();
    }

    private static TableConflict drivers(List<TableConflict> tables) {
        return tables.stream().filter(table -> DRIVERS.equals(table.id())).findFirst().orElseThrow();
    }

    private static TableConflict rates(List<TableConflict> tables) {
        return tables.stream().filter(table -> RATES.equals(table.id())).findFirst().orElseThrow();
    }
}
