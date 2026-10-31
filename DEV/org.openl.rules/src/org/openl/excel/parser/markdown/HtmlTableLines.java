package org.openl.excel.parser.markdown;

import org.commonmark.node.Block;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.SourceSpan;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.AbstractBlockParserFactory;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;

/**
 * An HTML table of a Markdown document, known by its lines alone, as {@link TableLines} knows a GFM table.
 *
 * <p>The table starts at a line that opens it with {@code <table}, three spaces of indent at most, and ends at the
 * line that closes it with {@code </table>}. A blank line ends it too, as it ends a block of HTML in GFM: a table cut
 * so is not closed.
 */
final class HtmlTableLines extends CustomBlock {

    private static final String OPEN = "<table";
    private static final String CLOSE = "</table>";

    private int firstLine = -1;
    private int lastLine;
    private boolean closed;

    /** Returns the line that opens the table. */
    int firstLine() {
        return firstLine;
    }

    /** Returns the last line of the table: the line that closes it, or the last one before the end of the block. */
    int lastLine() {
        return lastLine;
    }

    /** Tells whether a line of the table closes it. */
    boolean closed() {
        return closed;
    }

    private static boolean closes(CharSequence line) {
        var text = line.toString();
        if (text.contains(CLOSE)) {
            return true;
        }
        // The closing tag in upper or mixed case: a closing tag whose name starts with 'ta'
        var at = text.indexOf("</");
        while (at >= 0 && !(startsTa(text, at + 2) && text.regionMatches(true, at, CLOSE, 0, CLOSE.length()))) {
            at = text.indexOf("</", at + 2);
        }
        return at >= 0;
    }

    private static boolean startsTa(String text, int at) {
        return at + 1 < text.length() && (text.charAt(at) | 0x20) == 't' && (text.charAt(at + 1) | 0x20) == 'a';
    }

    /**
     * Starts a table at a line that opens one.
     */
    static final class Factory extends AbstractBlockParserFactory {

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            if (state.getIndent() >= 4) {
                return BlockStart.none();
            }
            var line = state.getLine().getContent();
            var start = state.getNextNonSpaceIndex();
            var end = start + OPEN.length();
            var opens = line.length() >= end
                    && OPEN.equalsIgnoreCase(line.subSequence(start, end).toString())
                    && (line.length() == end || line.charAt(end) == '>' || Character.isWhitespace(line.charAt(end)));
            if (!opens) {
                return BlockStart.none();
            }
            var parser = new LineParser();
            parser.block.closed = closes(line);
            return BlockStart.of(parser).atIndex(state.getIndex());
        }
    }

    /**
     * Takes the lines of a table, keeping their numbers alone.
     */
    private static final class LineParser extends AbstractBlockParser {

        private final HtmlTableLines block = new HtmlTableLines();

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public BlockContinue tryContinue(ParserState state) {
            if (block.closed || state.isBlank()) {
                return BlockContinue.none();
            }
            block.closed = closes(state.getLine().getContent());
            return BlockContinue.atIndex(state.getIndex());
        }

        @Override
        public void addSourceSpan(SourceSpan sourceSpan) {
            // Only the numbers of the first and the last line are kept, whatever the number of rows
            if (block.firstLine < 0) {
                block.firstLine = sourceSpan.getLineIndex();
            }
            block.lastLine = sourceSpan.getLineIndex();
        }
    }
}
