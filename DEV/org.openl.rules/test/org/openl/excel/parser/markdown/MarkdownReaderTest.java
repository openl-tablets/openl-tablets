package org.openl.excel.parser.markdown;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.excel.parser.MergedCell.MERGE_WITH_LEFT;
import static org.openl.excel.parser.MergedCell.MERGE_WITH_UP;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.excel.parser.ExcelParseException;
import org.openl.excel.parser.ExcelReaderFactory;
import org.openl.excel.parser.FolderUtils;
import org.openl.rules.table.GridRegion;

class MarkdownReaderTest {

    private static final String GREETING = FolderUtils.getResourcesFolder() + "markdown/Greetings/greeting.md";
    private static final char BYTE_ORDER_MARK = 0xFEFF;

    @Test
    void headerAndPropertiesAreLaidOutAsInExcel() {
        var text = """
                ---
                table: Rules String Greeting(Integer hour)
                description: Greeting by the hour of the day
                effectiveDate: 2024-01-01
                state: [CA, NY]
                ---

                Documentation before the table.

                | Hour  | Greeting       |
                |-------|----------------|
                | 0-11  | Good Morning   |
                | 12-17 | Good Afternoon |
                """;

        // The last cell of each row of a body narrower than the properties is merged over the columns they take
        assertArrayEquals(new Object[][]{
                {"Rules String Greeting(Integer hour)", MERGE_WITH_LEFT, MERGE_WITH_LEFT},
                {"properties", "description", "Greeting by the hour of the day"},
                {MERGE_WITH_UP, "effectiveDate", "2024-01-01"},
                {MERGE_WITH_UP, "state", "CA,NY"},
                {"Hour", "Greeting", MERGE_WITH_LEFT},
                {"0-11", "Good Morning", MERGE_WITH_LEFT},
                {"12-17", "Good Afternoon", MERGE_WITH_LEFT}}, cellsOf("Greeting.md", text));
    }

    @Test
    void cellsAreTakenAsWritten() {
        var text = """
                ---
                table: Data String values
                ---
                | Value                  |
                |------------------------|
                | plain                  |
                | `= a * b`              |
                | a \\| b                 |
                | line<br>break          |
                | *kept*                 |
                |                        |
                | `` code with ` tick `` |
                """;

        assertArrayEquals(new Object[][]{
                {"Data String values"},
                {"Value"},
                {"plain"},
                {"= a * b"},
                {"a | b"},
                {"line\nbreak"},
                {"*kept*"},
                {null},
                {"code with ` tick"}}, cellsOf("values.md", text));
    }

    @Test
    void mergesBecomeMarkers() {
        var text = """
                ---
                table: Rules Double BasePremium(Integer age)
                merges: [A1:B1, C2:C3]
                ---
                | Age |    | Premium |
                |-----|----|---------|
                | 16  | 24 | 1200    |
                | 25  | 64 |         |
                """;

        assertArrayEquals(new Object[][]{
                {"Rules Double BasePremium(Integer age)", MERGE_WITH_LEFT, MERGE_WITH_LEFT},
                {"Age", MERGE_WITH_LEFT, "Premium"},
                {"16", "24", "1200"},
                {"25", "64", MERGE_WITH_UP}}, cellsOf("BasePremium.md", text));
    }

    @Test
    void codeBlockIsTheOnlyCellAndTheRestIsDocumentation() {
        var text = """
                ---
                table: Method String hello(String name)
                ---

                Says hello.

                ```java
                return "Hello, " + name;
                ```

                | Not | rules |
                |-----|-------|
                """;

        assertArrayEquals(new Object[][]{
                {"Method String hello(String name)"},
                {"return \"Hello, \" + name;"}}, cellsOf("hello.md", text));
    }

    @Test
    void fileWithoutFrontMatterIsDocumentation() {
        try (var notes = read("rules/Rating/README.md", "# Notes\n\n| a | b |\n|---|---|\n")) {
            var sheets = notes.getSheets();

            assertEquals(1, sheets.size());
            assertEquals(0, notes.getCells(sheets.getFirst()).length);
        }
    }

    @Test
    void sheetIsNamedAfterTheFolderOfTheFile() throws IOException {
        try (var byPath = ExcelReaderFactory.sequentialFactory().create(GREETING);
             var stream = new FileInputStream(GREETING);
             var byUri = new MarkdownReader(stream, "file:/projects/Example/rules/Rating/Premium/premium.md");
             var alone = read("premium.md", "")) {
            var sheet = byPath.getSheets().getFirst();

            assertEquals("Greetings", sheet.getName());
            assertEquals(0, sheet.getIndex());
            assertEquals(0, sheet.getFirstRowNum());
            assertEquals(0, sheet.getFirstColNum());
            assertEquals("Premium", byUri.getSheets().getFirst().getName());
            assertEquals("premium", alone.getSheets().getFirst().getName());
        }
    }

    @Test
    void sourceKeepsNoFormatting() {
        try (var reader = ExcelReaderFactory.sequentialFactory().create(GREETING)) {
            var region = new GridRegion(0, 0, 3, 2);
            var styles = reader.getTableStyles(reader.getSheets().getFirst(), region);

            assertFalse(reader.isUse1904Windowing());
            assertSame(region, styles.getRegion());
            assertNull(styles.getStyle(0, 0));
            assertNull(styles.getFont(0, 0));
            assertNull(styles.getComment(0, 0));
            assertNull(styles.getFormula(0, 0));
        }
    }

    @Test
    void factoryTellsMarkdownByName() throws IOException {
        var factory = ExcelReaderFactory.sequentialFactory();
        try (var byPath = factory.create(GREETING);
             var stream = new FileInputStream(GREETING);
             var byStream = factory.create(stream, "file:/projects/rules/Greetings/greeting.md")) {
            assertInstanceOf(MarkdownReader.class, byPath);
            assertInstanceOf(MarkdownReader.class, byStream);
            assertArrayEquals(byPath.getCells(byPath.getSheets().getFirst()),
                    byStream.getCells(byStream.getSheets().getFirst()));
        }
    }

    @Test
    void factoryRefusesAMissingMarkdownFile() {
        var factory = ExcelReaderFactory.sequentialFactory();

        assertThrows(ExcelParseException.class, () -> factory.create("missing/rules.md"));
    }

    @Test
    void factoryClosesTheStreamItReadAMarkdownFileFrom() {
        var closed = new AtomicBoolean();
        var stream = new ByteArrayInputStream("---\ntable: Data String values\n---\n| a |\n|---|\n".getBytes(
                StandardCharsets.UTF_8)) {
            @Override
            public void close() {
                closed.set(true);
            }
        };

        try (var reader = ExcelReaderFactory.sequentialFactory().create(stream, "jar:file:/p.zip!/rules/Main/a.md")) {
            assertEquals("Main", reader.getSheets().getFirst().getName());
        }

        assertTrue(closed.get());
    }

    @Test
    void byteOrderMarkIsSkipped() {
        var text = BYTE_ORDER_MARK + "---\ntable: Data String values\n---\n| a |\n|---|\n";

        assertArrayEquals(new Object[][]{{"Data String values"}, {"a"}}, cellsOf("Bom.md", text));
    }

    // {h} opens the front matter of a table X, {/h} closes it, and \n is a line break.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = ';', quoteCharacter = '"', textBlock = """
            header without body; {h}{/h}\\nText; 1; must be followed by a table or a code block
            not closed;          {h}| a |\\n|---|\\n; 1; not closed
            extra cells;    {h}{/h}| a | b |\\n|---|---|\\n| 1 | 2 | 3 |\\n; 6; more cells
            split table;    {h}{/h}| a |\\n|---|\\n| 1 |\\n\\n| 2 |\\n; 8; blank line splits
            merge outside;  {h}merges: [A1:C1]\\n{/h}| a | b |\\n|---|---|\\n; 3; A1:C1 is outside
            merge below;    {h}merges: A1:A2\\n{/h}| a |\\n|---|\\n; 3; A1:A2 is outside
            merges overlap; {h}merges: [A1:B1, B1:B2]\\n{/h}| a | |\\n|---|---|\\n| 1 | |\\n; 3; A1:B1 and B1:B2 overlap
            covered value;  {h}merges: [A1:B1]\\n{/h}| a | b |\\n|---|---|\\n; 3; B1 has a value
            bad range;      {h}merges: [1A]\\n{/h}| a |\\n|---|\\n; 3; '1A' is not a cell range
            reversed range; {h}merges: [B2:A1]\\n{/h}| a |\\n|---|\\n; 3; from its top left
            row too far;    {h}merges: [A99999999999]\\n{/h}| a |\\n|---|\\n; 3; is not a cell range
            column too far; {h}merges: [ZZZZZZZZZ1]\\n{/h}| a |\\n|---|\\n; 3; is not a cell range
            nested value;   {h}state: {a: b}\\n{/h}| a |\\n|---|\\n; 3; text or a list
            complex key;    {h}[a]: b\\n{/h}| a |\\n|---|\\n; 3; single word
            no table key;   ---\\ndescription: x\\n---\\n| a |\\n|---|\\n; 1; no 'table' key
            empty table key; ---\\ntable:\\n---\\n| a |\\n|---|\\n; 1; no 'table' key
            list;            ---\\n- a\\n---\\n| a |\\n|---|\\n; 1; set of 'key: value' lines
            invalid yaml;    ---\\ntable: [a, b]\\n- c\\n---\\n| a |\\n|---|\\n; 3; Invalid YAML
            """)
    void formatErrorsTellTheLine(String name, String text, int line, String message) {
        var source = text.replace("{h}", "---\\ntable: Rules X()\\n")
                .replace("{/h}", "---\\n")
                .replace("\\n", "\n");

        var error = assertThrows(MarkdownFormatException.class, () -> read("Broken.md", source));

        assertTrue(error.getMessage().startsWith("Broken.md, line %d: ".formatted(line)), error::getMessage);
        assertTrue(error.getMessage().contains(message), error::getMessage);
    }

    @Test
    void countsCellsAsGfmSplitsThem() {
        assertEquals(1, MarkdownTableParser.countCells("| a |"));
        assertEquals(1, MarkdownTableParser.countCells("a |"));
        assertEquals(1, MarkdownTableParser.countCells("| a"));
        assertEquals(2, MarkdownTableParser.countCells("a | b"));
        assertEquals(2, MarkdownTableParser.countCells(" | a \\| b | c | "));
        assertEquals(1, MarkdownTableParser.countCells("| a \\|"));
        // GFM escapes the pipe after a backslash even when another backslash stands before it
        assertEquals(2, MarkdownTableParser.countCells("| a \\\\| b | c |"));
        assertEquals(1, MarkdownTableParser.countCells("| a \\b |"));
    }

    private static Object[][] cellsOf(String name, String text) {
        try (var reader = read(name, text)) {
            return reader.getCells(reader.getSheets().getFirst());
        }
    }

    private static MarkdownReader read(String name, String text) {
        return new MarkdownReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), name);
    }
}
