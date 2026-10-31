package org.openl.excel.parser.markdown;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * Reads a GFM table of a Markdown file: a row to a line, its cells parted by pipes, none of them merged.
 *
 * <p>A cell is taken as written, not rendered as Markdown. A cell wrapped in backticks as a whole is unwrapped,
 * {@code \|} gives a vertical bar and {@code <br>} a line break. A row gives every cell written in it, even those GFM
 * drops past the cells of the first row, so the layout tells of them.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class GfmTableReader implements MarkdownTableReader {

    private static final Pattern LINE_BREAK = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);

    /** The lines of the file. */
    private final List<String> lines;
    /** The line of each row, the delimiter row left out. */
    private final List<Integer> rowLines;

    /**
     * Returns the rows of the table. A row is cut out of its line when it is asked for, so a table of millions of
     * cells holds no row it is not working on.
     */
    @Override
    public List<TableRow> getRows() {
        return new AbstractList<>() {
            @Override
            public TableRow get(int index) {
                var line = rowLines.get(index);
                return new TableRow(new TableFile.Block(line, line), cellsOf(lines.get(line)));
            }

            @Override
            public int size() {
                return rowLines.size();
            }
        };
    }

    /**
     * Returns the cells of a row, as GFM splits it.
     */
    static List<TableCell> cellsOf(String row) {
        var bounds = TableLines.cellBounds(row);
        var cells = new ArrayList<TableCell>(bounds.length / 2);
        for (var cell = 0; 2 * cell < bounds.length; cell++) {
            cells.add(TableCell.of(cellText(row, bounds[2 * cell], bounds[2 * cell + 1])));
        }
        return cells;
    }

    /**
     * Returns the value of a cell standing between two places of a row, or {@code null} for a blank cell.
     */
    private static @Nullable String cellText(String row, int from, int to) {
        var start = from;
        var end = to;
        while (start < end && Character.isWhitespace(row.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(row.charAt(end - 1))) {
            end--;
        }
        return start == end ? null : cellText(row.substring(start, end));
    }

    /**
     * Returns the value of a cell written as {@code source}, or {@code null} for a blank cell.
     */
    static @Nullable String cellText(String source) {
        var text = unwrapCode(source.strip().replace("\\|", "|"));
        return StringUtils.trimToNull(text.indexOf('<') < 0 ? text : LINE_BREAK.matcher(text).replaceAll("\n"));
    }

    private static String unwrapCode(String text) {
        var ticks = 0;
        while (ticks < text.length() && text.charAt(ticks) == '`') {
            ticks++;
        }
        if (ticks == 0 || text.length() <= 2 * ticks) {
            return text;
        }
        var fence = text.substring(0, ticks);
        var code = text.substring(ticks, text.length() - ticks);
        return text.endsWith(fence) && !code.contains(fence) && !code.endsWith("`") ? code : text;
    }
}
