package org.openl.excel.parser.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.openl.util.FileTypeHelper;

/**
 * A table laid out on a sheet is written back into its Markdown file, and the file reads back as the same table.
 */
class MarkdownTableWriterTest {

    private static final String GREETING = """
            ---
            table: SmartRules String greeting(Integer hour)
            description: Greeting by the hour
            ---

            Picks a greeting.

            | Hour  | Greeting       |
            |-------|----------------|
            | 0-11  | Good Morning   |
            | 12-17 | Good Afternoon |
            | 18-23 | Good Evening   |

            Notes after the table.
            """;

    private static final String HELLO = """
            ---
            table: Method String hello(String name)
            ---

            ```java
            return "Hello, " + name;
            ```
            """;

    @Test
    void fileIsWrittenAsItIsWhenNothingChanged() throws IOException {
        try (var files = Files.walk(Path.of("test/rules/markdown"))) {
            var checked = 0;
            for (var file : files.filter(path -> FileTypeHelper.isMarkdownFile(path.toString())).toList()) {
                checked += checkUnchanged(file) ? 1 : 0;
            }
            assertTrue(checked > 5, "Too few files checked: " + checked);
        }
        assertEquals(GREETING, write(GREETING, cellsOf(GREETING)));
        assertEquals(HELLO, write(HELLO, cellsOf(HELLO)));
    }

    private static boolean checkUnchanged(Path file) throws IOException {
        var text = Files.readString(file, StandardCharsets.UTF_8);
        try {
            var table = parse(text).table();
            var cells = SheetLayout.cellsOf(table);

            assertEquals(table, SheetLayout.tableOf(cells), file.toString());
            assertEquals(text, write(text, cells), file.toString());
            return true;
        } catch (MarkdownFormatException | IllegalStateException e) {
            // A broken file or a file of documentation holds no table to write
            return false;
        }
    }

    @Test
    void changedValueRewritesItsRowAlone() {
        var cells = set(cellsOf(GREETING), 3, 1, "Morning");

        assertEquals(GREETING.replace("| 0-11  | Good Morning   |", "| 0-11  | Morning        |"),
                write(GREETING, cells));
    }

    @Test
    void valueWiderThanItsColumnIsNotPadded() {
        var cells = set(cellsOf(GREETING), 3, 1, "Good Morning, Sunshine");

        assertEquals(GREETING.replace("| 0-11  | Good Morning   |", "| 0-11  | Good Morning, Sunshine |"),
                write(GREETING, cells));
    }

    @Test
    void rowOfATableWrittenWithTheShortestDelimiterKeepsItsEmptyCells() {
        var text = """
                ---
                table: Data String values
                ---

                | a | b |  |
                |---|---|---|
                | 1 |  | 3 |
                """;

        // A table with no column aligned has its changed rows written as a new file writes them
        assertEquals(text.replace("| 1 |  | 3 |", "| 2 |  | 3 |"), write(text, set(cellsOf(text), 2, 0, "2")));
    }

    @Test
    void insertedRowLeavesTheOtherRowsAsTheyAre() {
        var cells = insertRow(cellsOf(GREETING), 4, "11-12", "Lunch");

        assertEquals(GREETING.replace("| 12-17 |", "| 11-12 | Lunch          |\n| 12-17 |"), write(GREETING, cells));
    }

    @Test
    void removedRowLeavesTheOtherRowsAsTheyAre() {
        var cells = removeRow(cellsOf(GREETING), 4);

        assertEquals(GREETING.replace("| 12-17 | Good Afternoon |\n", ""), write(GREETING, cells));
    }

    @Test
    void addedColumnRewritesTheTable() {
        var cells = set(set(cellsOf(GREETING), 2, 2, "Note"), 3, 2, "early");

        assertEquals(GREETING.replace("""
                | Hour  | Greeting       |
                |-------|----------------|
                | 0-11  | Good Morning   |
                | 12-17 | Good Afternoon |
                | 18-23 | Good Evening   |
                """, """
                | Hour | Greeting | Note |
                |---|---|---|
                | 0-11 | Good Morning | early |
                | 12-17 | Good Afternoon |  |
                | 18-23 | Good Evening |  |
                """), write(GREETING, cells));
    }

    @Test
    void mergeIsWrittenIntoTheFrontMatter() {
        var cells = merge(set(cellsOf(GREETING), 5, 0, null), new CellRangeAddress(4, 5, 0, 0));
        var text = write(GREETING, cells);

        assertEquals(GREETING.replace("description: Greeting by the hour\n",
                        "description: Greeting by the hour\nmerges: [A3:A4]\n")
                .replace("| 18-23 | Good Evening   |", "|       | Good Evening   |"), text);
        assertEquals(List.of(CellRangeAddress.valueOf("A3:A4")), parse(text).table().merges());
    }

    @Test
    void unmergeRemovesTheMergesFromTheFrontMatter() {
        var merged = write(GREETING, merge(set(cellsOf(GREETING), 5, 0, null), new CellRangeAddress(4, 5, 0, 0)));
        var cells = cellsOf(merged);
        var unmerged = new SheetCells(cells.values(), cells.merges()
                .stream()
                .filter(range -> range.getFirstRow() < 4)
                .toList());

        assertEquals(GREETING.replace("| 18-23 | Good Evening   |", "|       | Good Evening   |"),
                write(merged, unmerged));
    }

    @Test
    void changedPropertyRewritesItsKeyAlone() {
        var cells = set(cellsOf(GREETING), 1, 2, "Greeting by the hour of the day");

        assertEquals(GREETING.replace("description: Greeting by the hour\n",
                "description: Greeting by the hour of the day\n"), write(GREETING, cells));
    }

    @Test
    void propertiesAreAddedAndRemoved() {
        var table = parse(GREETING).table();
        var properties = new LinkedHashMap<String, String>();
        properties.put("description", "Greeting by the hour");
        properties.put("effectiveDate", "2024-01-01");
        properties.put("tags", "morning: early");
        properties.put("note", "");
        var added = write(GREETING, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(GREETING.replace("description: Greeting by the hour\n", """
                description: Greeting by the hour
                effectiveDate: 2024-01-01
                tags: "morning: early"
                note: ""
                """), added);
        assertEquals(properties, parse(added).table().properties());

        properties.remove("description");
        var removed = write(added, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(added.replace("description: Greeting by the hour\n", ""), removed);
    }

    @Test
    void changedHeaderRewritesItsKeyAlone() {
        var cells = set(cellsOf(GREETING), 0, 0, "SmartRules String greet(Integer hour)");

        assertEquals(GREETING.replace("greeting(Integer hour)", "greet(Integer hour)"), write(GREETING, cells));
    }

    @Test
    void codeBlockKeepsItsFence() {
        var cells = set(cellsOf(HELLO), 1, 0, "return \"Hi, \" + name;");

        assertEquals(HELLO.replace("Hello, ", "Hi, "), write(HELLO, cells));
    }

    @Test
    void codeHoldingTheFenceGetsALongerOne() {
        var code = "// ```\nreturn name;";
        var text = write(HELLO, set(cellsOf(HELLO), 1, 0, code));

        assertTrue(text.contains("````java\n// ```\nreturn name;\n````\n"), text);
        assertEquals(code, parse(text).table().body().getFirst().getFirst());
    }

    @Test
    void valuesReadBackAsWritten() {
        var values = List.of("a | b", "line 1\nline 2", "`quoted`", "a \\| b", "**not bold**", "1 ``` 2");
        var cells = cellsOf(GREETING);
        for (var i = 0; i < values.size(); i++) {
            cells = insertRow(cells, 3 + i, "x" + i, values.get(i));
        }

        var body = parse(write(GREETING, cells)).table().body();

        for (var i = 0; i < values.size(); i++) {
            assertEquals(values.get(i), body.get(1 + i).get(1));
        }
    }

    @Test
    void lineBreaksAndByteOrderMarkOfTheFileAreKept() {
        var text = "﻿" + GREETING.replace("\n", "\r\n");
        var cells = set(cellsOf(GREETING), 3, 1, "Morning");

        var written = write(text, cells);

        assertEquals("﻿" + GREETING.replace("| 0-11  | Good Morning   |", "| 0-11  | Morning        |")
                .replace("\n", "\r\n"), written);
    }

    @Test
    void layoutWithoutMarkdownFormIsRefused() {
        var greeting = cellsOf(GREETING);
        assertRefused(GREETING, "The first cell of the table must hold its header", set(greeting, 0, 0, null));
        assertRefused(HELLO, "The header of the table takes one row",
                merge(set(cellsOf(HELLO), 1, 0, null), new CellRangeAddress(0, 1, 0, 0)));
        assertRefused(GREETING, "Only the header and the properties stand above the body, but D1 holds a value",
                set(greeting, 0, 3, "x"));
        assertRefused(GREETING, "The merged range C2:C3 cannot be written in a Markdown file",
                merge(greeting, new CellRangeAddress(1, 2, 2, 2)));
        assertRefused(GREETING, "The cell B4 has a value, but the merged range A4:B4 covers it",
                merge(greeting, new CellRangeAddress(3, 3, 0, 1)));
        assertRefused(GREETING, "The table must have a row under its header",
                new SheetCells(new String[][]{{"Data String values"}}, List.of()));
        assertRefused(GREETING, "The property 'merges' cannot be written: the name is taken",
                set(greeting, 1, 1, "merges"));
        assertRefused(GREETING, "The property in row 2 must have a name in a cell of its own",
                set(greeting, 1, 1, null));
        var narrow = new @Nullable String[][]{
                {"Spreadsheet SpreadsheetResult terms()"}, {"properties", "description"}, {"Step", "Value"},
                {"Count", "= 1"}};
        assertRefused(GREETING, "A property needs the third column for its value, but the table is 2 columns wide",
                new SheetCells(narrow, List.of()));
        assertRefused(GREETING, "The value 'a <br> b' cannot be written in a Markdown table",
                set(greeting, 3, 1, "a <br> b"));
    }

    private static void assertRefused(String text, String message, SheetCells cells) {
        var error = assertThrows(MarkdownWriteException.class, () -> write(text, cells));
        assertEquals(message, error.getMessage());
    }

    // Helpers

    private static TableFile parse(String text) {
        return MarkdownTableParser.parse(text, "t.md").orElseThrow(IllegalStateException::new);
    }

    private static SheetCells cellsOf(String text) {
        return SheetLayout.cellsOf(parse(text).table());
    }

    private static String write(String text, SheetCells cells) {
        return MarkdownTableWriter.write(text, "t.md", cells);
    }

    private static SheetCells set(SheetCells cells, int row, int column, @Nullable String value) {
        var values = copy(cells.values(), row + 1, column + 1);
        values[row][column] = value;
        return new SheetCells(values, cells.merges());
    }

    private static SheetCells merge(SheetCells cells, CellRangeAddress range) {
        var merges = new ArrayList<>(cells.merges());
        merges.add(range);
        return new SheetCells(cells.values(), merges);
    }

    private static SheetCells insertRow(SheetCells cells, int at, String... values) {
        var rows = new ArrayList<>(Arrays.asList(cells.values()));
        rows.add(at, values.clone());
        return new SheetCells(rows.toArray(String[][]::new), shifted(cells.merges(), at, 1));
    }

    private static SheetCells removeRow(SheetCells cells, int at) {
        var rows = new ArrayList<>(Arrays.asList(cells.values()));
        rows.remove(at);
        return new SheetCells(rows.toArray(String[][]::new), shifted(cells.merges(), at, -1));
    }

    private static List<CellRangeAddress> shifted(List<CellRangeAddress> merges, int at, int by) {
        return merges.stream()
                .map(range -> range.getFirstRow() < at ? range : new CellRangeAddress(range.getFirstRow() + by,
                        range.getLastRow() + by,
                        range.getFirstColumn(),
                        range.getLastColumn()))
                .toList();
    }

    private static @Nullable String[][] copy(@Nullable String[][] values, int rows, int columns) {
        var copy = new @Nullable String[Math.max(values.length, rows)][];
        for (var row = 0; row < copy.length; row++) {
            var source = row < values.length ? values[row] : new String[0];
            copy[row] = Arrays.copyOf(source, Math.max(source.length, row == rows - 1 ? columns : 0));
        }
        return copy;
    }
}
