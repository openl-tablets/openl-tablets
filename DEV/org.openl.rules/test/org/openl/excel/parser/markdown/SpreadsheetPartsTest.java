package org.openl.excel.parser.markdown;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.excel.parser.MergedCell.MERGE_WITH_LEFT;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A Spreadsheet is written in parts, a caption between each two, and reads as one table with each caption a row
 * merged across it.
 */
class SpreadsheetPartsTest {

    private static final String PREMIUM = """
            ---
            table: Spreadsheet SpreadsheetResult premium(Double base)
            ---

            Yearly premium.

            | Step     | Value              |
            |----------|--------------------|
            | Base     | `= base`           |
            | Discount | `= $Base * 0.1`    |

            Final Calculation

            | Step     | Value              |
            |----------|--------------------|
            | Total    | `= $Base - $Discount` |

            Notes after the table.
            """;

    @Test
    void partsReadAsOneTableWithACaptionMergedAcross() {
        // The description above the first part is a property, so the last cell of each row reaches its column
        assertArrayEquals(new Object[][]{
                {"Spreadsheet SpreadsheetResult premium(Double base)", MERGE_WITH_LEFT, MERGE_WITH_LEFT},
                {"properties", "description", "Yearly premium."},
                {"Step", "Value", MERGE_WITH_LEFT},
                {"Base", "= base", MERGE_WITH_LEFT},
                {"Discount", "= $Base * 0.1", MERGE_WITH_LEFT},
                {"Final Calculation", MERGE_WITH_LEFT, MERGE_WITH_LEFT},
                {"Total", "= $Base - $Discount", MERGE_WITH_LEFT}}, cellsOf(PREMIUM));
        var table = parse(PREMIUM).table();

        assertEquals(List.of(CellRangeAddress.valueOf("A4:B4")), table.merges());
        // The text after the last part is documentation, not a caption
        assertEquals(5, table.body().size());
    }

    @Test
    void mergesCountTheRowsOfTheTablePutTogether() {
        var text = PREMIUM.replace("table: Spreadsheet SpreadsheetResult premium(Double base)\n",
                        "table: Spreadsheet SpreadsheetResult premium(Double base)\nmerges: [B5:B6]\n")
                .replace("| Total    | `= $Base - $Discount` |",
                        "| Total    | `= $Base - $Discount` |\n| Rounded  |  |");
        var table = parse(text).table();

        assertEquals(List.of(CellRangeAddress.valueOf("A4:B4"), CellRangeAddress.valueOf("B5:B6")), table.merges());
        assertEquals(List.of("merges"), parse(text).keys().stream()
                .filter(key -> "B5:B6".equals(key.value()))
                .map(TableFile.Key::name)
                .toList());
    }

    @Test
    void captionsFollowOneAnotherAndAPartMayHoldTheFirstRowAlone() {
        var text = """
                ---
                table: Calc SpreadsheetResult total()
                ---
                | Step | Value |
                |---|---|

                Inputs

                Outputs

                | Step | Value |
                |---|---|
                | Total | 1 |

                End

                | Step | Value |
                |---|---|
                """;
        var table = parse(text).table();

        assertEquals(Arrays.asList(Arrays.asList("Step", "Value"),
                Arrays.asList("Inputs", null),
                Arrays.asList("Outputs", null),
                Arrays.asList("Total", "1"),
                Arrays.asList("End", null)), table.body());
        assertEquals(List.of(CellRangeAddress.valueOf("A2:B2"), CellRangeAddress.valueOf("A3:B3"),
                CellRangeAddress.valueOf("A5:B5")), table.merges());
        assertEquals(text, write(text, SheetLayout.cellsOf(table)));
        assertEquals(table, parse(MarkdownTableWriter.newFile(table)).table());
    }

    @Test
    void onlyASpreadsheetGoesOnInTheTablesAfterIt() {
        var text = PREMIUM.replace("Spreadsheet SpreadsheetResult premium(Double base)", "Data Steps steps");
        var file = parse(text);

        assertEquals(3, file.table().body().size());
        assertTrue(file.table().merges().isEmpty());
        assertTrue(file.problems().isEmpty(), file.problems()::toString);
    }

    @Test
    void tableThatDoesNotRepeatTheFirstRowIsNoPart() {
        var file = parse(PREMIUM.replace("""
                Final Calculation

                | Step     | Value              |""", """
                Final Calculation

                | Steps    | Value              |"""));

        assertEquals(3, file.table().body().size());
        assertEquals(List.of(new MarkdownProblem(14,
                "The table does not repeat the first row of the Spreadsheet above, so it is no part of it",
                null)), file.problems());
    }

    @Test
    void fileIsWrittenAsItIsWhenNothingChanged() {
        assertEquals(PREMIUM, write(PREMIUM, sheetOf(PREMIUM)));
    }

    @Test
    void changedValueOfALaterPartRewritesItsRowAlone() {
        var cells = set(SheetLayout.cellsOf(parse(PREMIUM).table()), 6, 1, "= round($Base - $Discount, 2)");

        assertEquals(PREMIUM.replace("| Total    | `= $Base - $Discount` |",
                "| Total    | = round($Base - $Discount, 2) |"), write(PREMIUM, cells));
    }

    @Test
    void rowMergedAcrossBecomesACaptionBetweenTwoParts() {
        var whole = """
                ---
                table: Spreadsheet SpreadsheetResult premium(Double base)
                ---

                | Step     | Value         |
                |----------|---------------|
                | Base     | `= base`      |
                | Summary  |               |
                | Total    | `= $Base * 2` |
                """;
        var cells = merge(SheetLayout.cellsOf(parse(whole).table()), new CellRangeAddress(3, 3, 0, 1));
        var text = write(whole, cells);

        assertEquals("""
                ---
                table: Spreadsheet SpreadsheetResult premium(Double base)
                ---

                | Step     | Value         |
                |----------|---------------|
                | Base     | `= base`      |

                Summary

                | Step     | Value         |
                |----------|---------------|
                | Total    | `= $Base * 2` |
                """, text);
        assertEquals(SheetLayout.tableOf(cells), parse(text).table());
    }

    @Test
    void captionUnmergedJoinsTheParts() {
        var cells = SheetLayout.cellsOf(parse(PREMIUM).table());
        // The last cell of the row still reaches the column of the properties, as the layout gives it
        var unmerged = merge(new SheetCells(cells.values(), cells.merges()
                .stream()
                .filter(range -> range.getFirstRow() != 5)
                .toList()), new CellRangeAddress(5, 5, 1, 2));

        assertEquals(PREMIUM.replace("""
                | Discount | `= $Base * 0.1`    |

                Final Calculation

                | Step     | Value              |
                |----------|--------------------|
                | Total    | `= $Base - $Discount` |""", """
                | Discount | `= $Base * 0.1`    |
                | Final Calculation |                    |
                | Total    | `= $Base - $Discount` |"""), write(PREMIUM, unmerged));
    }

    @ParameterizedTest
    @ValueSource(strings = {"# Total", "- Total", "> Total", "1. Total", "| Total", "Two\nlines", "```"})
    void captionThatReadsBackAsOtherMarkdownIsAMergedRange(String caption) {
        var cells = set(SheetLayout.cellsOf(parse(PREMIUM).table()), 5, 0, caption);
        var text = write(PREMIUM, cells);

        assertTrue(text.contains("merges:\n  - A4:B4\n"), text);
        assertFalse(text.contains("Final Calculation"), text);
        assertEquals(SheetLayout.tableOf(cells), parse(text).table());
    }

    @Test
    void newFileWritesTheCaptionsAsParagraphs() {
        var properties = new LinkedHashMap<String, String>();
        var body = List.of(Arrays.asList("Step", "Value"),
                Arrays.<@Nullable String>asList("Base", "= base"),
                Arrays.<@Nullable String>asList("Summary", null),
                Arrays.<@Nullable String>asList("Total", "= $Base * 2"));
        var table = new RulesTable("Spreadsheet SpreadsheetResult premium(Double base)", properties, body,
                List.of(CellRangeAddress.valueOf("A3:B3")));
        var text = MarkdownTableWriter.newFile(table);

        assertEquals("""
                ---
                table: Spreadsheet SpreadsheetResult premium(Double base)
                ---

                | Step | Value |
                |---|---|
                | Base | = base |

                Summary

                | Step | Value |
                |---|---|
                | Total | = $Base * 2 |
                """, text);
        assertEquals(table, parse(text).table());
    }

    @Test
    void captionOfATableOfAnotherTypeIsAMergedRange() {
        var properties = new LinkedHashMap<String, String>();
        var body = List.of(Arrays.asList("name", "value"), Arrays.<@Nullable String>asList("Summary", null));
        var table = new RulesTable("Data Steps steps", properties, body, List.of(CellRangeAddress.valueOf("A2:B2")));

        assertTrue(MarkdownTableWriter.newFile(table).contains("merges:\n  - A2:B2\n"));
    }

    // Helpers

    private static TableFile parse(String text) {
        return MarkdownTableParser.parse(text).orElseThrow(IllegalStateException::new);
    }

    private static Object[][] cellsOf(String text) {
        try (var reader = new MarkdownReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                "premium.md")) {
            return reader.getCells(reader.getSheets().getFirst());
        }
    }

    private static SheetCells sheetOf(String text) {
        return SheetLayout.cellsOf(parse(text).table());
    }

    private static String write(String text, SheetCells cells) {
        return MarkdownTableWriter.write(text, cells);
    }

    private static SheetCells set(SheetCells cells, int row, int column, @Nullable String value) {
        var values = Arrays.stream(cells.values()).map(String[]::clone).toArray(String[][]::new);
        values[row][column] = value;
        return new SheetCells(values, cells.merges());
    }

    private static SheetCells merge(SheetCells cells, CellRangeAddress range) {
        var merges = new ArrayList<>(cells.merges());
        merges.add(range);
        return new SheetCells(cells.values(), merges);
    }
}
