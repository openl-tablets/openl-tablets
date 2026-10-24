package org.openl.excel.parser.markdown;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Document;
import org.commonmark.node.Node;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TableLinesTest {

    private static final Parser LINES = Parser.builder()
            .customBlockParserFactory(new TableLines.Factory())
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();
    private static final Parser GFM = Parser.builder()
            .extensions(List.of(TablesExtension.builder().maxCells(null).build()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
            .build();

    // \n is a line break and \t a tab. The reader takes a table of the document itself, so only such a table has its
    // cells compared: a table inside a quote or a list has its lines compared.
    @ParameterizedTest
    @ValueSource(strings = {
            "| a | b |\\n|---|---|\\n| 1 | 2 |",
            "a | b\\n--- | ---\\n1 | 2",
            "| a \\| b | c |\\n|---|---|\\n| x \\\\| y | z |\\n| \\b | c\\ |",
            "| a | b |\\n| :-- | --: |\\n| :-: | -- |",
            "Text above\\n| a | b |\\n|---|---|\\n| 1 | 2 |",
            "| a |\\n|---|\\n| 1 |\\na row of text\\n\\nA paragraph",
            "| a |\\n|---|\\n| 1 |\\n|\\n| 2 |",
            "| a |\\n|---|\\n| 1 |\\n   |   \\nafter",
            "| a |\\n|---|\\n| 1 |\\n\\n| 2 |",
            "| a | b |\\n|---|\\n| 1 | 2 |",
            "| a |\\n|---|---|\\n| 1 | 2 |",
            "| a | b |\\n|---|---|\\n| 1 |\\n| 1 | 2 | 3 |\\n| 1 | 2 | x",
            "| a | b |\\n|---|---|\\n|| 2 |\\n| 1 ||",
            "|\\ta\\t|\\tb\\t|\\n|\\t---\\t|---|\\n|\\t1\\t|\\t2\\t|",
            "  | a |\\n  |---|\\n  | 1 |",
            "    | a |\\n    |---|\\n    | 1 |",
            "# Title\\n| a |\\n|---|\\n| 1 |\\n# Next",
            "> | a |\\n> |---|\\n> | 1 |\\nlazy | row",
            "- | a |\\n  |---|\\n  | 1 |",
            "a\\n---",
            "| a |\\n|-x-|",
            "| a | b |\\n||---|---|",
            "| a | b |\\n|--- ---|",
            "| a | b |\\n|:|---|",
            "| a |\\n-",
            "| a |\\n|---|\\n```\\n| code |\\n```",
            "```\\n| a |\\n|---|\\n```\\n| b |\\n|---|",
            "| a |\\n|---|\\n| 1 |\\n| a |\\n|---|\\n| 2 |",
            "| `a | b` | c |\\n|---|---|"
    })
    void findsTablesAndCellsAsTheGfmTablesExtension(String document) {
        var text = document.replace("\\n", "\n").replace("\\t", "\t");
        var lines = List.of(text.split("\n", -1));

        assertEquals(gfmTables(GFM.parse(text), lines), tableLines(LINES.parse(text), lines));
    }

    @Test
    void cutsCellsAtUnescapedPipes() {
        assertArrayEquals(new int[]{1, 4, 5, 8}, TableLines.cellBounds("| a | b |"));
        assertArrayEquals(new int[]{0, 2, 3, 5}, TableLines.cellBounds("a | b"));
        assertArrayEquals(new int[]{1, 9}, TableLines.cellBounds("| a \\| b |"));
        assertArrayEquals(new int[]{1, 1, 2, 5}, TableLines.cellBounds("|| b |"));
        assertArrayEquals(new int[]{1, 4, 5, 7}, TableLines.cellBounds("| a | x"));
    }

    @Test
    void countsTheColumnsOfADelimiterRow() {
        assertEquals(2, TableLines.delimiterColumns("|---|:--:|"));
        assertEquals(2, TableLines.delimiterColumns("--- | ---"));
        assertEquals(1, TableLines.delimiterColumns("|-"));
        assertEquals(0, TableLines.delimiterColumns("---"));
        assertEquals(0, TableLines.delimiterColumns("||---|"));
        assertEquals(0, TableLines.delimiterColumns("|:|"));
        assertEquals(0, TableLines.delimiterColumns("|-x-|"));
        assertEquals(0, TableLines.delimiterColumns("--- ---"));
    }

    /** The tables of a document read by the GFM tables extension: the line of each row and the text of each cell. */
    private static List<List<String>> gfmTables(Node document, List<String> lines) {
        var tables = new ArrayList<List<String>>();
        each(document, TableBlock.class, (table, ofTheDocument) -> {
            var rows = new ArrayList<String>();
            for (var section = table.getFirstChild(); section != null; section = section.getNext()) {
                for (var row = section.getFirstChild(); row != null; row = row.getNext()) {
                    var line = row.getSourceSpans().getFirst().getLineIndex();
                    var cells = new ArrayList<@Nullable String>();
                    for (var cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
                        var spans = cell.getSourceSpans();
                        var span = spans.isEmpty() ? null : spans.getFirst();
                        cells.add(span == null ? null : text(lines.get(span.getLineIndex()),
                                span.getColumnIndex(),
                                span.getColumnIndex() + span.getLength()));
                    }
                    rows.add(ofTheDocument ? line + " " + cells : String.valueOf(line));
                }
            }
            tables.add(rows);
        });
        return tables;
    }

    /** The tables of a document as the lines of their rows, the cells cut out of the lines as the reader cuts them. */
    private static List<List<String>> tableLines(Node document, List<String> lines) {
        var tables = new ArrayList<List<String>>();
        each(document, TableLines.class, (table, ofTheDocument) -> {
            var rowLines = table.rowLines();
            var width = TableLines.cellBounds(lines.get(rowLines.getFirst())).length / 2;
            var rows = new ArrayList<String>();
            for (var line : rowLines) {
                var bounds = TableLines.cellBounds(lines.get(line));
                var cells = new ArrayList<@Nullable String>();
                for (var column = 0; column < width; column++) {
                    cells.add(2 * column < bounds.length
                            ? text(lines.get(line), bounds[2 * column], bounds[2 * column + 1])
                            : null);
                }
                rows.add(ofTheDocument ? line + " " + cells : String.valueOf(line));
            }
            tables.add(rows);
        });
        return tables;
    }

    /** Returns the text of a cell without the spaces around it, or {@code null} for a blank cell. */
    private static @Nullable String text(String line, int start, int end) {
        var text = line.substring(start, end).strip();
        return text.isEmpty() ? null : text;
    }

    /** Runs an action on each node of a type, telling whether it is a block of the document itself. */
    private static <T extends Node> void each(Node node, Class<T> type, BiConsumer<T, Boolean> action) {
        for (var child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (type.isInstance(child)) {
                action.accept(type.cast(child), node instanceof Document);
            }
            each(child, type, action);
        }
    }
}
