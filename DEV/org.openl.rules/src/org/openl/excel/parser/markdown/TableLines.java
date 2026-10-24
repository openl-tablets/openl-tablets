package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commonmark.node.Block;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.SourceSpan;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.AbstractBlockParserFactory;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;
import org.commonmark.text.Characters;

/**
 * A GFM table of a Markdown document, known by its lines alone.
 *
 * <p>The table is found by the rules of GFM tables, as the tables extension of commonmark finds it, but it holds no
 * node or text for a row or a cell: the reader cuts the cells out of the lines of the file. A table of millions of
 * cells costs the numbers of its lines.
 *
 * <p>A table starts at a delimiter row, such as {@code |---|:--:|}, right under a line of a paragraph that holds a
 * pipe and has no more cells than the delimiter row has columns: that line is the first row. The table goes on while
 * a line holds a pipe, and a line of text right after a row is a row too. A line of a lone pipe ends the table, and so
 * does a blank line or the start of another block.
 */
final class TableLines extends CustomBlock {

    /** The line of the delimiter row, then the line of each row after it. */
    private final List<Integer> lines = new ArrayList<>();

    /** Returns the line of each row, the first row included. */
    List<Integer> rowLines() {
        var rows = new ArrayList<Integer>(lines.size());
        rows.add(getSourceSpans().getFirst().getLineIndex());
        rows.addAll(lines.subList(1, lines.size()));
        return rows;
    }

    /** Returns the line of the delimiter row under the first row. */
    int delimiterLine() {
        return lines.getFirst();
    }

    /**
     * Returns where the cells of a row stand in it, as GFM splits the row: the start and the end of each cell in turn,
     * the spaces around the text included.
     *
     * <p>A pipe splits cells unless a backslash stands before it. The pipes at the ends of the row are no cells, but a
     * text after the last pipe is one.
     */
    static int[] cellBounds(CharSequence row) {
        var start = Characters.skipSpaceTab(row, 0, row.length());
        var end = row.length();
        if (start < end && row.charAt(start) == '|') {
            start++;
            end = Characters.skipSpaceTabBackwards(row, end - 1, start) + 1;
        }
        // Every cell but the last ends at a pipe
        var bounds = new int[2 * (pipesIn(row, start, end) + 1)];
        var count = 0;
        var cellStart = start;
        var hasText = false;
        var i = start;
        while (i < end) {
            var c = row.charAt(i);
            var next = i + 1;
            if (c == '|') {
                bounds[count++] = cellStart;
                bounds[count++] = i;
                cellStart = next;
                hasText = false;
            } else {
                hasText = true;
                // An escaped pipe is text of the cell
                next += c == '\\' && next < end && row.charAt(next) == '|' ? 1 : 0;
            }
            i = next;
        }
        if (hasText) {
            bounds[count++] = cellStart;
            bounds[count++] = row.length();
        }
        return count == bounds.length ? bounds : Arrays.copyOf(bounds, count);
    }

    private static int pipesIn(CharSequence row, int start, int end) {
        var pipes = 0;
        for (var i = start; i < end; i++) {
            pipes += row.charAt(i) == '|' ? 1 : 0;
        }
        return pipes;
    }

    /**
     * Returns the number of columns of a delimiter row, or 0 for a line that is no delimiter row.
     *
     * <p>Each column is a run of dashes, a colon allowed at either end of it, and the columns are parted by single
     * pipes. A delimiter row has a pipe even for a table of one column; spaces and tabs may stand around the pipes.
     */
    static int delimiterColumns(CharSequence line) {
        var columns = 0;
        var pipes = 0;
        var hasPipe = false;
        var i = 0;
        while (i < line.length()) {
            var c = line.charAt(i);
            if (c == '|') {
                pipes++;
                hasPipe = true;
                i++;
                if (pipes > 1) {
                    return 0;
                }
            } else if (c == '-' || c == ':') {
                var end = columnEnd(line, i);
                if (end < 0 || pipes == 0 && columns > 0) {
                    return 0;
                }
                columns++;
                pipes = 0;
                i = end;
            } else if (c == ' ' || c == '\t') {
                i++;
            } else {
                return 0;
            }
        }
        return hasPipe ? columns : 0;
    }

    /**
     * Returns where a column of a delimiter row ends, or -1 when it has no dash.
     */
    private static int columnEnd(CharSequence line, int start) {
        var i = line.charAt(start) == ':' ? start + 1 : start;
        var dashes = i;
        while (i < line.length() && line.charAt(i) == '-') {
            i++;
        }
        if (i == dashes) {
            return -1;
        }
        return i < line.length() && line.charAt(i) == ':' ? i + 1 : i;
    }

    /**
     * Starts a table at a delimiter row under a line of a paragraph with a pipe.
     */
    static final class Factory extends AbstractBlockParserFactory {

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            var paragraph = matchedBlockParser.getParagraphLines().getLines();
            if (paragraph.isEmpty()) {
                return BlockStart.none();
            }
            var firstRow = paragraph.getLast().getContent();
            if (Characters.find('|', firstRow, 0) < 0) {
                return BlockStart.none();
            }
            var line = state.getLine().getContent();
            var columns = delimiterColumns(line.subSequence(state.getIndex(), line.length()));
            if (columns == 0 || columns < cellBounds(firstRow).length / 2) {
                return BlockStart.none();
            }
            return BlockStart.of(new LineParser()).atIndex(state.getIndex()).replaceParagraphLines(1);
        }
    }

    /**
     * Takes the lines of a table, keeping their numbers alone.
     */
    private static final class LineParser extends AbstractBlockParser {

        private final TableLines block = new TableLines();
        private boolean lazyRows = true;

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public boolean canHaveLazyContinuationLines() {
            return lazyRows;
        }

        @Override
        public BlockContinue tryContinue(ParserState state) {
            var line = state.getLine().getContent();
            var start = state.getNextNonSpaceIndex();
            var pipe = Characters.find('|', line, start);
            if (pipe < 0) {
                return BlockContinue.none();
            }
            if (pipe == start && Characters.skipSpaceTab(line, pipe + 1, line.length()) == line.length()) {
                // A lone pipe is no row, not even a row of text after the table
                lazyRows = false;
                return BlockContinue.none();
            }
            return BlockContinue.atIndex(state.getIndex());
        }

        @Override
        public void addSourceSpan(SourceSpan sourceSpan) {
            block.lines.add(sourceSpan.getLineIndex());
        }
    }
}
