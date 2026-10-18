package org.openl.excel.parser.markdown;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * The indent of a cell of a Markdown table: non-breaking spaces before its text. {@code &nbsp;&nbsp;SET} is
 * {@code SET} indented by one level, as a workbook indents a cell.
 *
 * <p>A level is two non-breaking spaces, each written as the entity {@code &nbsp;}; an odd one left over makes a level
 * of its own, and spaces between them do not count. A table trims the spaces around the text of a cell but keeps the
 * entities, and a Markdown viewer shows them as the indent.
 *
 * <p>The indent carries the structure of a TBasic and a ColumnMatch table: an operation indented under another is part
 * of it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownIndent {

    /** A non-breaking space, as a file writes it. */
    private static final String SPACE = "&nbsp;";
    private static final int SPACES_PER_LEVEL = 2;

    /**
     * A text of a cell and its indent.
     *
     * @param text  the text without the spaces that indent it, or {@code null} for no text
     * @param level the levels of the indent, zero for none
     */
    record Indented(@Nullable String text, int level) {
    }

    /**
     * Reads the indent of the text of a cell.
     */
    static Indented of(@Nullable String cell) {
        if (cell == null) {
            return new Indented(null, 0);
        }
        var spaces = 0;
        var position = 0;
        while (position < cell.length()) {
            if (cell.startsWith(SPACE, position)) {
                spaces++;
                position += SPACE.length();
            } else if (cell.charAt(position) == ' ') {
                position++;
            } else {
                break;
            }
        }
        return spaces == 0
                ? new Indented(cell, 0)
                : new Indented(StringUtils.trimToNull(cell.substring(position)), (spaces + 1) / SPACES_PER_LEVEL);
    }

    /**
     * Returns the text of a cell indented by the given levels.
     *
     * @return the text, or {@code null} for no text: a cell without text has no indent to keep
     */
    static @Nullable String write(@Nullable String text, int level) {
        return text == null || level <= 0 ? text : SPACE.repeat(level * SPACES_PER_LEVEL) + text;
    }
}
