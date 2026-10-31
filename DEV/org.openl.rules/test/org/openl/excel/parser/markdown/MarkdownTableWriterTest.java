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
import java.util.Map;

import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.openl.util.FileTypeHelper;

/**
 * A table laid out on a sheet is written back into its Markdown file, and the file reads back as the same table.
 */
class MarkdownTableWriterTest {

    private static final String GREETING_TABLE = """
            | Hour  | Greeting       |
            |-------|----------------|
            | 0-11  | Good Morning   |
            | 12-17 | Good Afternoon |
            | 18-23 | Good Evening   |
            """;

    private static final String GREETING = """
            ---
            table: SmartRules String greeting(Integer hour)
            properties:
              category: Greetings
            ---

            > Picks a greeting.

            %s
            Notes after the table.
            """.formatted(GREETING_TABLE);

    /** The greeting with the afternoon and the evening under one hour, which merges their cells of the hour. */
    private static final String MERGED = GREETING.replace(GREETING_TABLE, """
            <table>
            <tr><td>Hour</td><td>Greeting</td></tr>
            <tr><td>0-11</td><td>Good Morning</td></tr>
            <tr><td rowspan="2">12-17</td><td>Good Afternoon</td></tr>
            <tr><td>Good Evening</td></tr>
            </table>
            """);

    /** A table described in two blocks of text, with comments between them. */
    private static final String DESCRIBED = """
            ---
            table: SmartRules String greeting(Integer hour)
            properties:
              category: Greetings
            ---

            Picks a greeting
            by the hour of the day.

            > A comment for the authors.

            <!-- A note no preview shows -->

            Morning ends at noon.

            | Hour  | Greeting     |
            |-------|--------------|
            | 0-11  | Good Morning |

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
        // The row repeats the merges of the row beside it, as the editor lays it down
        var cells = merge(insertRow(cellsOf(GREETING), 4, "11-12", "Lunch"), new CellRangeAddress(4, 4, 1, 2));

        assertEquals(GREETING.replace("| 12-17 |", "| 11-12 | Lunch          |\n| 12-17 |"), write(GREETING, cells));
    }

    @Test
    void removedRowLeavesTheOtherRowsAsTheyAre() {
        var cells = removeRow(cellsOf(GREETING), 4);

        assertEquals(GREETING.replace("| 12-17 | Good Afternoon |\n", ""), write(GREETING, cells));
    }

    @Test
    void addedColumnRewritesTheTable() {
        var cells = set(set(bodyUnmerged(cellsOf(GREETING)), 2, 2, "Note"), 3, 2, "early");

        assertEquals(GREETING.replace(GREETING_TABLE, """
                | Hour | Greeting | Note |
                |---|---|---|
                | 0-11 | Good Morning | early |
                | 12-17 | Good Afternoon |  |
                | 18-23 | Good Evening |  |
                """), write(GREETING, cells));
    }

    @Test
    void mergeTurnsTheTableIntoAnHtmlTable() {
        var cells = merge(set(cellsOf(GREETING), 5, 0, null), new CellRangeAddress(4, 5, 0, 0));
        var text = write(GREETING, cells);

        // A GFM table holds no merged cell
        assertEquals(MERGED, text);
        assertEquals(List.of(CellRangeAddress.valueOf("A3:A4")), parse(text).table().merges());
    }

    @Test
    void mergeRewritesTheRowsOfTheMergedCellAlone() {
        // A row whose cells and spans are the same keeps its line, aligned by hand here
        var aligned = MERGED.replace("<tr><td>Hour</td>", "<tr>  <td>Hour</td>");
        var cells = merge(set(cellsOf(aligned), 5, 1, null), new CellRangeAddress(4, 5, 1, 1));

        assertEquals(aligned.replace("""
                <tr><td rowspan="2">12-17</td><td>Good Afternoon</td></tr>
                <tr><td>Good Evening</td></tr>
                """, """
                <tr><td rowspan="2">12-17</td><td rowspan="2">Good Afternoon</td></tr>
                <tr></tr>
                """), write(aligned, cells));
    }

    @Test
    void lastUnmergeTurnsTheTableBackIntoAGfmTable() {
        var cells = cellsOf(MERGED);
        var unmerged = new SheetCells(cells.values(), cells.merges()
                .stream()
                .filter(range -> !range.equals(new CellRangeAddress(4, 5, 0, 0)))
                .toList());

        // The table is written anew, as a new file writes it
        assertEquals(GREETING.replace(GREETING_TABLE, """
                | Hour | Greeting |
                |---|---|
                | 0-11 | Good Morning |
                | 12-17 | Good Afternoon |
                |  | Good Evening |
                """), write(MERGED, unmerged));
    }

    @Test
    void htmlTableKeepsTheLinesThatFrameItsRows() {
        var framed = MERGED.replace("<table>\n", "<table>\n<tbody>\n").replace("</table>\n", "</tbody>\n</table>\n");
        var cells = set(cellsOf(framed), 3, 1, "Morning");

        assertEquals(framed.replace("<td>Good Morning</td>", "<td>Morning</td>"), write(framed, cells));
    }

    @Test
    void htmlTableWithACellToALineIsWrittenAnewOnAChange() {
        // A row keeps its line only when it is the one row of its line
        var celled = MERGED.replace("<tr><td>Hour</td><td>Greeting</td></tr>",
                "<tr>\n  <th>Hour</th>\n  <th>Greeting</th>\n</tr>");
        var cells = set(cellsOf(celled), 3, 1, "Morning");

        assertEquals(MERGED.replace("<td>Good Morning</td>", "<td>Morning</td>"), write(celled, cells));
    }

    @Test
    void htmlTableReadWithoutAPartOfItIsWrittenAnewOnAChange() {
        // The span reaches past the table: kept as written, the row would span the rows added below it
        var broken = MERGED.replace("rowspan=\"2\"", "rowspan=\"5\"");
        var cells = set(cellsOf(broken), 3, 1, "Morning");

        assertEquals(MERGED.replace("<td>Good Morning</td>", "<td>Morning</td>"), write(broken, cells));
    }

    @Test
    void mergesKeyIsAKeyTheTableIsReadWithout() {
        var stray = GREETING.replace("category: Greetings\n", "category: Greetings\nmerges: [A3:A4]\n");
        var file = parse(stray);

        assertEquals(List.of(), file.table().merges());
        assertTrue(file.problems().getFirst().problem().contains("written in an HTML table"), file::toString);
        // The key stays in the file as written, as any key the table is read without
        var merged = merge(set(cellsOf(stray), 5, 0, null), new CellRangeAddress(4, 5, 0, 0));
        assertEquals(MERGED.replace("category: Greetings\n", "category: Greetings\nmerges: [A3:A4]\n"),
                write(stray, merged));
    }

    @Test
    void changedPropertyRewritesItsKeyAlone() {
        var cells = set(cellsOf(GREETING), 1, 2, "Greetings of the day");

        assertEquals(GREETING.replace("category: Greetings\n",
                "category: Greetings of the day\n"), write(GREETING, cells));
    }

    @Test
    void propertiesAreAddedAndRemoved() {
        var table = parse(GREETING).table();
        var properties = new LinkedHashMap<String, String>();
        properties.put("category", "Greetings");
        properties.put("effectiveDate", "2024-01-01");
        properties.put("tags", "morning: early");
        properties.put("note", "");
        var added = write(GREETING, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(GREETING.replace("  category: Greetings\n", """
                  category: Greetings
                  effectiveDate: 2024-01-01
                  tags: "morning: early"
                  note: ""
                """), added);
        assertEquals(properties, parse(added).table().properties());

        properties.remove("category");
        var removed = write(added, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(added.replace("  category: Greetings\n", ""), removed);
    }

    @Test
    void lastPropertyRemovedTakesThePropertiesKeyAlong() {
        var table = parse(GREETING).table();

        var written = write(GREETING, SheetLayout.cellsOf(
                new RulesTable(table.header(), new LinkedHashMap<>(), table.body(), table.merges())));

        assertEquals(GREETING.replace("properties:\n  category: Greetings\n", ""), written);
    }

    @Test
    void newPropertyIsIndentedAsTheOthers() {
        var indented = GREETING.replace("  category:", "    category:");
        var table = parse(indented).table();
        var properties = new LinkedHashMap<>(table.properties());
        properties.put("tags", "morning");

        var written = write(indented, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(indented.replace("    category: Greetings\n",
                "    category: Greetings\n    tags: morning\n"), written);
    }

    @Test
    void propertiesOnTheLineOfTheirKeyAreWrittenOneToALine() {
        var inline = GREETING.replace("properties:\n  category: Greetings\n",
                "properties: {category: Greetings}\n");
        var table = parse(inline).table();
        var properties = new LinkedHashMap<>(table.properties());
        properties.put("tags", "morning");

        var written = write(inline, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(GREETING.replace("  category: Greetings\n",
                "  category: Greetings\n  tags: morning\n"), written);
    }

    @Test
    void keyTheTableIsReadWithoutStaysInTheFile() {
        var stray = GREETING.replace("---\n\n> Picks", "state: CA\n---\n\n> Picks");
        var cells = set(cellsOf(stray), 0, 0, "SmartRules String greet(Integer hour)");

        assertEquals(stray.replace("greeting(Integer hour)", "greet(Integer hour)"), write(stray, cells));
    }

    @Test
    void changedHeaderRewritesItsKeyAlone() {
        var cells = set(cellsOf(GREETING), 0, 0, "SmartRules String greet(Integer hour)");

        assertEquals(GREETING.replace("greeting(Integer hour)", "greet(Integer hour)"), write(GREETING, cells));
    }

    @Test
    void descriptionIsTheTextAboveTheTableWithoutTheComments() {
        var properties = new LinkedHashMap<String, String>();
        properties.put("description", "Picks a greeting\nby the hour of the day.\n\nMorning ends at noon.");
        properties.put("category", "Greetings");

        assertEquals(properties, parse(DESCRIBED).table().properties());
        assertEquals(DESCRIBED, write(DESCRIBED, cellsOf(DESCRIBED)));
        // The text above the code of a Method describes it too
        assertEquals(Map.of("description", "Says hello."),
                parse(HELLO.replace("---\n\n```", "---\n\nSays hello.\n\n```")).table().properties());
    }

    @Test
    void changedDescriptionIsWrittenWhereItStoodAndTheCommentsStay() {
        var cells = set(cellsOf(DESCRIBED), 1, 2, "Greets by the hour.");

        assertEquals(DESCRIBED.replace("""
                Picks a greeting
                by the hour of the day.
                """, "Greets by the hour.\n").replace("Morning ends at noon.\n\n", ""), write(DESCRIBED, cells));
    }

    @Test
    void descriptionAddedGoesRightAfterTheFrontMatter() {
        var table = parse(GREETING).table();
        var properties = new LinkedHashMap<String, String>();
        properties.put("description", "Greets by the hour.");
        properties.putAll(table.properties());

        var written = write(GREETING, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(GREETING.replace("---\n\n> Picks", "---\n\nGreets by the hour.\n\n> Picks"), written);
        assertEquals(properties, parse(written).table().properties());
    }

    @Test
    void descriptionTakenAwayLeavesTheComments() {
        var table = parse(DESCRIBED).table();
        var properties = new LinkedHashMap<>(table.properties());
        properties.remove("description");

        var written = write(DESCRIBED, SheetLayout.cellsOf(
                new RulesTable(table.header(), properties, table.body(), table.merges())));

        assertEquals(DESCRIBED.replace("""
                Picks a greeting
                by the hour of the day.

                """, "").replace("Morning ends at noon.\n\n", ""), written);
    }

    @Test
    void descriptionLineThatWouldStartAQuoteACodeBlockOrATableIsEscaped() {
        var description = "> not a comment\n```\n| not a table |\n  > indented";
        var table = new RulesTable("Datatype Adult", new LinkedHashMap<>(Map.of("description", description)),
                List.of(), List.of());

        var text = MarkdownTableWriter.newFile(table);

        assertEquals("""
                ---
                table: Datatype Adult
                ---

                \\> not a comment
                \\```
                \\| not a table |
                  \\> indented
                """, text);
        assertEquals(table, parse(text).table());
    }

    @Test
    void newFileWritesTheDescriptionAboveTheBody() {
        var table = parse(DESCRIBED).table();

        var text = MarkdownTableWriter.newFile(table);

        assertEquals("""
                ---
                table: SmartRules String greeting(Integer hour)
                properties:
                  category: Greetings
                ---

                Picks a greeting
                by the hour of the day.

                Morning ends at noon.

                | Hour | Greeting |
                |---|---|
                | 0-11 | Good Morning |
                """, text);
        assertEquals(table, parse(text).table());
    }

    @Test
    void bodyWrittenIntoATableWithAHeaderAloneFollowsItsDescription() {
        var headerAlone = """
                ---
                table: Datatype Adult extends Driver
                ---

                A driver of age.

                > A comment for the authors.
                """;
        var table = parse(headerAlone).table();
        var withBody = SheetLayout.cellsOf(new RulesTable(table.header(), table.properties(),
                List.of(List.of("Integer", "age")), List.of()));

        assertEquals(Map.of("description", "A driver of age."), table.properties());
        assertEquals(headerAlone.replace("of age.\n", "of age.\n\n| Integer | age |\n|---|---|\n"),
                write(headerAlone, withBody));
    }

    @Test
    void descriptionUnderThePropertiesIsAProblem() {
        var file = parse("""
                ---
                table: Datatype Adult
                properties:
                  description: Old
                  tags: adult
                ---

                New.
                """);

        assertEquals(Map.of("description", "New.", "tags", "adult"), file.table().properties());
        assertEquals(List.of(new MarkdownProblem(4, """
                The description of a table is the text between the front matter and the table, not a property \
                under 'properties'""", null)), file.problems());
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
    void valuesReadBackAsWrittenInAnHtmlTable() {
        var values = List.of("a <= b", "<b>not bold</b>", "a && b", "&amp; &lt;", "&nbsp;&nbsp;indented",
                "line 1\nline 2", "a <br> b", "a | b", "`quoted`", "</td>");
        var cells = cellsOf(MERGED);
        for (var i = 0; i < values.size(); i++) {
            cells = insertRow(cells, 3 + i, "x" + i, values.get(i));
        }

        var text = write(MERGED, cells);
        var body = parse(text).table().body();

        // What would read as a tag or a reference is escaped, the indent of a cell is not
        assertTrue(text.contains("<td>a <= b</td>"), text);
        assertTrue(text.contains("<td>&lt;b>not bold&lt;/b></td>"), text);
        assertTrue(text.contains("<td>&amp;amp; &amp;lt;</td>"), text);
        assertTrue(text.contains("<td>&nbsp;&nbsp;indented</td>"), text);
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
    void newFileHoldsTheTableAlone() {
        var table = parse(GREETING).table();
        var merged = SheetLayout.tableOf(merge(set(cellsOf(GREETING), 5, 0, null), new CellRangeAddress(4, 5, 0, 0)));

        var text = MarkdownTableWriter.newFile(table);

        assertEquals("""
                ---
                table: SmartRules String greeting(Integer hour)
                properties:
                  category: Greetings
                ---

                | Hour | Greeting |
                |---|---|
                | 0-11 | Good Morning |
                | 12-17 | Good Afternoon |
                | 18-23 | Good Evening |
                """, text);
        assertEquals(table, parse(text).table());
        // A table with merged cells is an HTML table
        assertEquals(MERGED.replace("> Picks a greeting.\n\n", "").replace("\nNotes after the table.\n", ""),
                MarkdownTableWriter.newFile(merged));
        assertEquals(merged, parse(MarkdownTableWriter.newFile(merged)).table());
    }

    @Test
    void bodyNarrowerThanThePropertiesKeepsItsWidth() {
        // A workbook merges the last cell of each row of the body over the columns the properties take
        var widened = cellsOf(GREETING);
        assertEquals(List.of("A1:C1", "B3:C3", "B4:C4", "B5:C5", "B6:C6"),
                widened.merges().stream().map(CellRangeAddress::formatAsString).toList());

        assertEquals(GREETING, write(GREETING, widened));
        assertEquals(parse(GREETING).table(), SheetLayout.tableOf(widened));

        // A body that leaves the columns of the properties empty takes them all
        var unmerged = bodyUnmerged(widened);
        var table = SheetLayout.tableOf(unmerged);
        assertEquals(3, table.width());
        assertEquals(unmerged.merges(), SheetLayout.cellsOf(table).merges());
    }

    @Test
    void tableWithAHeaderAloneIsItsFrontMatter() {
        var headerAlone = new SheetCells(new String[][]{{"Datatype Adult extends Driver"}}, List.of());
        var written = """
                ---
                table: Datatype Adult extends Driver
                ---
                """;

        assertEquals(written, MarkdownTableWriter.newFile(SheetLayout.tableOf(headerAlone)));
        // A body written into the file follows the front matter, and a body taken away leaves it alone
        var withBody = new SheetCells(new String[][]{{"Datatype Adult extends Driver"}, {"Integer", "age"}}, List.of());
        assertEquals(written + "\n| Integer | age |\n|---|---|\n", write(written, withBody));
        // The text after the front matter of a table with a header alone is its description; a comment stays
        assertEquals(written, write(written + "\nAn adult.\n", headerAlone));
        assertEquals(written + "\n> Notes.\n", write(written + "\n> Notes.\n", headerAlone));
        assertEquals("""
                ---
                table: Datatype Adult extends Driver
                ---

                Notes.
                """, write("""
                ---
                table: Datatype Adult extends Driver
                ---

                | Integer | age |
                |---|---|

                Notes.
                """, headerAlone).replace("\n\n\n", "\n\n"));
    }

    @Test
    void newFileWritesTheCodeOfAMethodInACodeBlock() {
        assertEquals(HELLO, MarkdownTableWriter.newFile(parse(HELLO).table()));
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
        assertRefused(GREETING, "The cell A5 has a value, but the merged range A4:A5 covers it",
                merge(greeting, new CellRangeAddress(3, 4, 0, 0)));
        assertRefused(GREETING, "The property 'table' cannot be written: the name is taken",
                set(greeting, 1, 1, "table"));
        assertRefused(GREETING, "The property in row 2 must have a name in a cell of its own",
                set(greeting, 1, 1, null));
        var narrow = new @Nullable String[][]{
                {"Spreadsheet SpreadsheetResult terms()"}, {"properties", "category"}, {"Step", "Value"},
                {"Count", "= 1"}};
        assertRefused(GREETING, "A property needs the third column for its value, but the table is 2 columns wide",
                new SheetCells(narrow, List.of()));
        assertRefused(GREETING, "The value 'a <br> b' cannot be written in a Markdown table",
                set(greeting, 3, 1, "a <br> b"));
        assertRefused("A file of documentation alone\n", "The file holds no table to write", greeting);
    }

    private static void assertRefused(String text, String message, SheetCells cells) {
        var error = assertThrows(MarkdownWriteException.class, () -> write(text, cells));
        assertEquals(message, error.getMessage());
    }

    // Helpers

    private static TableFile parse(String text) {
        return MarkdownTableParser.parse(text).orElseThrow(IllegalStateException::new);
    }

    private static SheetCells cellsOf(String text) {
        return SheetLayout.cellsOf(parse(text).table());
    }

    private static String write(String text, SheetCells cells) {
        return MarkdownTableWriter.write(text, cells);
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

    /** The cells with the body merged nowhere, the columns of the properties left empty. */
    private static SheetCells bodyUnmerged(SheetCells cells) {
        return new SheetCells(cells.values(),
                cells.merges().stream().filter(range -> range.getFirstRow() < 2).toList());
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
