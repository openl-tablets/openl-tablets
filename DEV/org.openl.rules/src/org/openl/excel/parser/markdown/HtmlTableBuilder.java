package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * Writes an HTML table, the table of a body with merged cells: a row to a line,
 * {@code <tr><td rowspan="2">0-11</td><td>Morning</td></tr>}, a merged cell spanning its rows and columns, and none of
 * the cells it covers. GitHub and GitLab show such a table as a workbook shows it.
 *
 * <p>A value is written as text that {@link HtmlTableReader} reads back as the value: a line break as {@code <br>},
 * {@code <} as {@code &lt;} before what would start a tag, and {@code &} as {@code &amp;} before what would read as a
 * reference other than {@code &nbsp;}, the indent of a cell. So {@code <= 14} and {@code a && b} stay as written.
 */
final class HtmlTableBuilder implements MarkdownTableBuilder {

    private static final String OPEN = "<table>";
    private static final String CLOSE = "</table>";
    /** What reads as a reference after a {@code &}. */
    private static final Pattern REFERENCE = Pattern.compile("&(#\\d+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]*);");
    private static final String INDENT = "&nbsp;";

    private final List<String> lines;
    private final List<String> closing;

    private HtmlTableBuilder(List<String> opening, List<String> closing) {
        this.lines = new ArrayList<>(opening);
        this.closing = closing;
    }

    /**
     * Returns a builder of a new table.
     */
    static HtmlTableBuilder of() {
        return new HtmlTableBuilder(List.of(OPEN), List.of(CLOSE));
    }

    /**
     * Returns a builder of a table written over a table of the file, which keeps the lines that open and close it.
     */
    static HtmlTableBuilder over(List<String> opening, List<String> closing) {
        return new HtmlTableBuilder(opening, closing);
    }

    @Override
    public HtmlTableBuilder row(List<TableCell> cells) {
        var row = new StringBuilder("<tr>");
        for (var cell : cells) {
            row.append("<td");
            if (cell.rowspan() > 1) {
                row.append(" rowspan=\"").append(cell.rowspan()).append('"');
            }
            if (cell.colspan() > 1) {
                row.append(" colspan=\"").append(cell.colspan()).append('"');
            }
            row.append(">").append(cell(cell.text())).append("</td>");
        }
        lines.add(row.append("</tr>").toString());
        return this;
    }

    @Override
    public HtmlTableBuilder written(String line) {
        lines.add(line);
        return this;
    }

    @Override
    public List<String> build() {
        var table = new ArrayList<>(lines);
        table.addAll(closing);
        return table;
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
        var text = new StringBuilder();
        for (var i = 0; i < normalized.length(); i++) {
            var c = normalized.charAt(i);
            if (c == '\n') {
                text.append("<br>");
            } else if (c == '<' && startsTag(normalized, i + 1)) {
                text.append("&lt;");
            } else if (c == '&' && startsReference(normalized, i)) {
                text.append("&amp;");
            } else {
                text.append(c);
            }
        }
        var written = text.toString();
        if (!Objects.equals(HtmlTableReader.valueOf(written), StringUtils.trimToNull(normalized))) {
            throw new MarkdownWriteException("The value '%s' cannot be written in a Markdown table".formatted(value));
        }
        return written;
    }

    private static boolean startsTag(String text, int at) {
        if (at >= text.length()) {
            return false;
        }
        var c = text.charAt(at);
        return Character.isLetter(c) || c == '/' || c == '!' || c == '?';
    }

    /** Tells a {@code &} that would read as a reference, the indent of a cell left out. */
    private static boolean startsReference(String text, int at) {
        return !text.startsWith(INDENT, at) && REFERENCE.matcher(text).region(at, text.length()).lookingAt();
    }
}
