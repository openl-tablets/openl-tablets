package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * A Markdown file of a rules table: the table, and where its parts stand in the text of the file.
 *
 * <p>Lines are counted from zero.
 *
 * <p>The text after the body is documentation and no part of it, however long it is: a write takes it from the file
 * as it stands (see {@link #tailOf} and {@link #tailStart}).
 *
 * @param lines          the lines of the file up to the end of the body, without their line breaks
 * @param lineSeparator  the line break the file is written with
 * @param table          the table read from the file
 * @param frontMatterEnd the line of the {@code ---} that closes the front matter
 * @param keys           the keys of the front matter, in the order they are written
 * @param propertyKeys   the keys under {@code properties}, in the order they are written
 * @param description    the blocks of text the description of the table is read from, in the order they are written
 * @param body           where the body of the table stands
 * @param problems       the problems of the file the table is read without
 */
record TableFile(List<String> lines,
                 String lineSeparator,
                 RulesTable table,
                 int frontMatterEnd,
                 List<Key> keys,
                 List<Key> propertyKeys,
                 List<Block> description,
                 Body body,
                 List<MarkdownProblem> problems) {

    /**
     * Returns the text after the body: from the line break that ends the body on, or an empty text when nothing
     * follows the body.
     *
     * @param text the text of the file, without its byte order mark
     */
    String tailOf(String text) {
        return text.substring(tailStart(text.length(), text::charAt));
    }

    /**
     * Returns where the text after the body starts in the bytes of the file: at the line break that ends the body, or
     * at the end when nothing follows the body.
     *
     * <p>The bytes are the file written in UTF-8, which writes a line break as a single byte that no other character
     * shares, so the line breaks are counted as in the text, even in bytes that are no UTF-8.
     *
     * @param content the bytes of the file
     * @param from    where the text starts, after the byte order mark
     */
    int tailStart(byte[] content, int from) {
        return from + tailStart(content.length - from, i -> content[from + i]);
    }

    private int tailStart(int length, IntUnaryOperator charAt) {
        var line = 0;
        var last = body.lastLine();
        var i = 0;
        while (i < length) {
            var c = charAt.applyAsInt(i);
            var next = i + 1;
            if (c == '\n' || c == '\r') {
                if (line == last) {
                    return i;
                }
                line++;
                // A carriage return and a line feed end one line
                next += c == '\r' && next < length && charAt.applyAsInt(next) == '\n' ? 1 : 0;
            }
            i = next;
        }
        return length;
    }

    /**
     * A block of text of the file, such as a paragraph.
     *
     * @param firstLine the first line of the block
     * @param lastLine  the last line of the block
     */
    record Block(int firstLine, int lastLine) {

        boolean holds(int line) {
            return firstLine <= line && line <= lastLine;
        }
    }

    /**
     * A key of the front matter.
     *
     * @param name  the name of the key
     * @param value the value as read: the header text, a property value, or the merged ranges joined with commas;
     *              empty for {@code properties} and for a key the table is read without
     * @param line  the line the key starts on
     */
    record Key(String name, String value, int line) {
    }

    /**
     * Where the body of the table stands.
     */
    sealed interface Body permits GfmBody, CodeBody, NoBody {

        /** Returns the first line of the body. */
        int firstLine();

        /** Returns the last line of the body. */
        int lastLine();
    }

    /**
     * A body written as GFM tables: one, or the parts of a Spreadsheet, a caption above each part after the first
     * (see {@link SpreadsheetParts}).
     *
     * @param parts the tables of the body in the order they are written
     */
    record GfmBody(List<Part> parts) implements Body {

        @Override
        public int firstLine() {
            return parts.getFirst().rowLines().getFirst();
        }

        @Override
        public int lastLine() {
            return parts.getLast().lastLine();
        }

        /**
         * Returns the line of each row of the table body: the rows of the first part, then for each part after it the
         * first line of each caption above it and its rows but the first, which repeats the first row of the table.
         */
        List<Integer> rowLines() {
            var rows = new ArrayList<>(parts.getFirst().rowLines());
            parts.stream().skip(1).forEach(part -> {
                part.captions().forEach(caption -> rows.add(caption.firstLine()));
                rows.addAll(part.rowLines().subList(1, part.rowLines().size()));
            });
            return rows;
        }

        /**
         * Returns the caption blocks by the rows of the table body they are.
         */
        Map<Integer, Block> captions() {
            var captions = new HashMap<Integer, Block>();
            var row = parts.getFirst().rowLines().size();
            for (var part : parts.subList(1, parts.size())) {
                for (var caption : part.captions()) {
                    captions.put(row++, caption);
                }
                row += part.rowLines().size() - 1;
            }
            return captions;
        }
    }

    /**
     * A GFM table of the body.
     *
     * @param captions      the captions written above the table, a paragraph each; none above the first table
     * @param rowLines      the line of each row, the first row included
     * @param delimiterLine the line of the delimiter row under the first row
     */
    record Part(List<Block> captions, List<Integer> rowLines, int delimiterLine) {

        int lastLine() {
            return Math.max(rowLines.getLast(), delimiterLine);
        }
    }

    /**
     * No body: a table with a header alone, such as a Datatype that adds no field to its parent. A body written into
     * the file starts right after the front matter and the description.
     *
     * @param firstLine the line after the front matter and the description
     */
    record NoBody(int firstLine) implements Body {

        @Override
        public int lastLine() {
            return firstLine - 1;
        }
    }

    /**
     * A body written as a fenced code block.
     *
     * @param firstLine the line of the opening fence
     * @param lastLine  the line of the closing fence, or the last line of the code when the fence is not closed
     * @param fence     the opening fence, such as {@code ```}
     * @param info      the text after the opening fence, such as {@code java}
     * @param indent    the number of spaces before the fence
     */
    record CodeBody(int firstLine, int lastLine, String fence, String info, int indent) implements Body {
    }
}
