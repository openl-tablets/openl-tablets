package org.openl.excel.parser.markdown;

import java.util.List;

/**
 * A Markdown file of a rules table: the table, and where its parts stand in the text of the file.
 *
 * <p>Lines are counted from zero.
 *
 * @param text           the text of the file
 * @param lines          the lines of the file, without their line breaks
 * @param lineSeparator  the line break the file is written with
 * @param table          the table read from the file
 * @param frontMatterEnd the line of the {@code ---} that closes the front matter
 * @param keys           the keys of the front matter, in the order they are written
 * @param propertyKeys   the keys under {@code properties}, in the order they are written
 * @param description    the blocks of text the description of the table is read from, in the order they are written
 * @param body           where the body of the table stands
 * @param problems       the problems of the file the table is read without
 */
record TableFile(String text,
                 List<String> lines,
                 String lineSeparator,
                 RulesTable table,
                 int frontMatterEnd,
                 List<Key> keys,
                 List<Key> propertyKeys,
                 List<Block> description,
                 Body body,
                 List<MarkdownProblem> problems) {

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
     * A body written as a GFM table.
     *
     * @param rowLines      the line of each row, the first row included
     * @param delimiterLine the line of the delimiter row under the first row
     */
    record GfmBody(List<Integer> rowLines, int delimiterLine) implements Body {

        @Override
        public int firstLine() {
            return rowLines.getFirst();
        }

        @Override
        public int lastLine() {
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
