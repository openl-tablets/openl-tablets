package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * Writes a GFM table: a row to a line, its cells parted by pipes, the delimiter row under the first row. A GFM table
 * holds no merged cell.
 *
 * <p>A row is written with its cells padded to the widths of the delimiter row, so a table aligned by hand stays
 * aligned while a value fits; the shortest delimiter, {@code ---}, which a new table is written with, aligns no
 * column. A value is written as text that {@link GfmTableReader} reads back as the value: a vertical bar as
 * {@code \|}, a line break as {@code <br>}, and a value that would read otherwise wrapped in backticks.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class GfmTableBuilder implements MarkdownTableBuilder {

    private static final String DELIMITER = "---";

    private final String delimiter;
    private final int[] widths;
    private final List<String> lines = new ArrayList<>();

    /**
     * Returns a builder of a new table, its columns aligned by nothing.
     *
     * @param width the number of columns
     */
    static GfmTableBuilder of(int width) {
        return new GfmTableBuilder("|" + (DELIMITER + "|").repeat(width), new int[width]);
    }

    /**
     * Returns a builder of a table written over a table of the file: it keeps the delimiter row, and pads a row to the
     * widths of its columns.
     *
     * @param delimiter the delimiter row of the file
     * @param width     the number of columns
     */
    static GfmTableBuilder over(String delimiter, int width) {
        return new GfmTableBuilder(delimiter, widths(delimiter, width));
    }

    @Override
    public GfmTableBuilder row(List<TableCell> cells) {
        add(line(cells));
        return this;
    }

    @Override
    public GfmTableBuilder written(String line) {
        add(line);
        return this;
    }

    @Override
    public List<String> build() {
        return List.copyOf(lines);
    }

    private void add(String line) {
        lines.add(line);
        if (lines.size() == 1) {
            lines.add(delimiter);
        }
    }

    private String line(List<TableCell> cells) {
        var row = new StringBuilder("|");
        for (var column = 0; column < cells.size(); column++) {
            var text = cell(cells.get(column).text());
            var width = column < widths.length ? widths[column] : 0;
            row.append(' ').append(text).append(" ".repeat(Math.max(width - text.length(), 0))).append(" |");
        }
        return row.toString();
    }

    /**
     * Returns the width of the text of each column, read from a delimiter row such as {@code |-------|---|}.
     */
    private static int[] widths(String delimiter, int width) {
        var row = delimiter.strip();
        var from = row.startsWith("|") ? 1 : 0;
        var to = row.endsWith("|") && row.length() > from ? row.length() - 1 : row.length();
        var segments = row.substring(from, to).split("\\|", -1);
        var widths = new int[width];
        for (var column = 0; column < Math.min(width, segments.length); column++) {
            var segment = segments[column];
            widths[column] = DELIMITER.equals(segment.strip()) ? 0 : Math.max(segment.length() - 2, 0);
        }
        return widths;
    }

    /**
     * Returns the text of a cell that reads back as the value.
     *
     * @throws MarkdownWriteException when no text of a cell reads back as the value
     */
    private static String cell(@Nullable String value) {
        if (value == null) {
            return "";
        }
        var normalized = MarkdownTableBuilder.normalized(value);
        var expected = StringUtils.trimToNull(normalized);
        var text = normalized.replace("|", "\\|").replace("\n", "<br>");
        if (Objects.equals(GfmTableReader.cellText(text), expected)) {
            return text;
        }
        // Wrapped in backticks, a value is taken as it is: one that starts with a backtick, for example
        var ticks = "`".repeat(longestRun(text, '`') + 1);
        var wrapped = ticks + " " + text + " " + ticks;
        if (Objects.equals(GfmTableReader.cellText(wrapped), expected)) {
            return wrapped;
        }
        throw new MarkdownWriteException("The value '%s' cannot be written in a Markdown table".formatted(value));
    }

    private static int longestRun(String text, char c) {
        var longest = 0;
        var run = 0;
        for (var i = 0; i < text.length(); i++) {
            run = text.charAt(i) == c ? run + 1 : 0;
            longest = Math.max(longest, run);
        }
        return longest;
    }
}
