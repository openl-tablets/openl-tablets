package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * Reads an HTML table of a Markdown file, the table of a body with merged cells: a merged cell is one {@code <td>}
 * that spans its rows and columns with {@code rowspan} and {@code colspan}, and a cell it covers is not written.
 *
 * <p>Any layout of the rows and the cells is read: a row to a line, as {@link HtmlTableBuilder} writes it, or a cell
 * to a line. {@code <th>} is read as {@code <td>}, and the closing tags a browser does without may be left out. A line
 * break of the file inside a cell, with the spaces around it, is a space, as a browser shows it.
 *
 * <p>A cell is taken as written, as a cell of a GFM table is. {@code <br>} is a line break; {@code &lt;},
 * {@code &gt;}, {@code &amp;}, {@code &quot;}, {@code &apos;} and the numeric references are the characters they stand
 * for; {@code &nbsp;} stays as written, as the indent of a cell (see {@link MarkdownIndent}). Any other tag is text of
 * the cell.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class HtmlTableReader implements MarkdownTableReader {

    /** The tags the table is read by; any other tag is text of a cell. */
    private static final List<String> TAGS = List.of("table", "thead", "tbody", "tfoot", "tr", "td", "th", "br");
    private static final Pattern SPAN = Pattern.compile("\\b(rowspan|colspan)\\s*=\\s*[\"']?(\\d{1,7})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_BREAK = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    /** A line break of the file inside a cell, with the spaces around it. */
    private static final Pattern SOURCE_BREAK = Pattern.compile("[ \\t]*\n[ \\t]*");
    /** The references a cell reads as characters; any other one stays as written. */
    private static final Pattern REFERENCE = Pattern.compile("&(lt|gt|amp|quot|apos|#\\d{1,7}|#[xX][0-9a-fA-F]{1,6});");

    /** The lines of the file. */
    private final List<String> lines;
    /** The line that opens the table. */
    private final int firstLine;
    /** The last line of the table. */
    private final int lastLine;

    @Override
    public List<TableRow> getRows() {
        var rows = new ArrayList<TableRow>();
        var tags = new Tags(rows);
        for (var line = firstLine; line <= lastLine; line++) {
            tags.read(lines.get(line), line);
        }
        tags.endRow(lastLine);
        return rows;
    }

    /**
     * Returns the value of a cell written as {@code html}, or {@code null} for a blank cell.
     */
    static @Nullable String valueOf(String html) {
        if (html.indexOf('<') < 0 && html.indexOf('&') < 0) {
            return StringUtils.trimToNull(html);
        }
        var text = LINE_BREAK.matcher(html).replaceAll("\n");
        return StringUtils.trimToNull(REFERENCE.matcher(text).replaceAll(HtmlTableReader::character));
    }

    private static String character(MatchResult reference) {
        var name = reference.group(1);
        var text = switch (name) {
            case "lt" -> "<";
            case "gt" -> ">";
            case "amp" -> "&";
            case "quot" -> "\"";
            case "apos" -> "'";
            default -> {
                var hex = name.length() > 1 && (name.charAt(1) == 'x' || name.charAt(1) == 'X');
                var code = Integer.parseInt(name.substring(hex ? 2 : 1), hex ? 16 : 10);
                // A reference to no character stays as written
                yield Character.isValidCodePoint(code) ? Character.toString(code) : reference.group();
            }
        };
        return Matcher.quoteReplacement(text);
    }

    /**
     * Reads the tags of the lines of a table into rows and cells.
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class Tags {

        private final List<TableRow> rows;
        private final StringBuilder cell = new StringBuilder();
        private @Nullable List<TableCell> cells;
        private int rowLine;
        private boolean inCell;
        private int rowspan;
        private int colspan;
        // The tag found last: its name, whether it opens, and where its attributes stand in its line
        private String name = "";
        private boolean opening;
        private int attributesFrom;
        private int attributesTo;

        void read(String text, int line) {
            var from = 0;
            var at = text.indexOf('<');
            while (at >= 0) {
                var end = tagAt(text, at);
                if (end > 0) {
                    text(text, from, at);
                    tag(text, line);
                    from = end;
                }
                at = text.indexOf('<', Math.max(end, at + 1));
            }
            text(text, from, text.length());
            text("\n", 0, 1);
        }

        /**
         * Reads the tag that starts at a place of a line, and returns the place after it, or -1 when no tag the table
         * is read by starts there.
         */
        private int tagAt(String text, int at) {
            var start = at + 1;
            opening = start >= text.length() || text.charAt(start) != '/';
            start += opening ? 0 : 1;
            var nameEnd = start;
            while (nameEnd < text.length() && isLetter(text.charAt(nameEnd))) {
                nameEnd++;
            }
            var close = text.indexOf('>', nameEnd);
            var known = nameOf(text, start, nameEnd);
            // A tag name ends where a letter, a digit or an underscore does not follow it
            var whole = nameEnd == text.length() || !isWordCharacter(text.charAt(nameEnd));
            if (known == null || !whole || close < 0) {
                return -1;
            }
            name = known;
            attributesFrom = nameEnd;
            attributesTo = close;
            return close + 1;
        }

        private static @Nullable String nameOf(String text, int from, int to) {
            for (var tag : TAGS) {
                if (tag.length() == to - from && sameLetters(text, from, tag)) {
                    return tag;
                }
            }
            return null;
        }

        /** Tells whether the letters at a place of a line spell a tag name, in whatever case. */
        private static boolean sameLetters(String text, int from, String tag) {
            var same = true;
            for (var i = 0; same && i < tag.length(); i++) {
                // A letter of either case and its lower case differ in the bit of the case alone
                same = (text.charAt(from + i) | 0x20) == tag.charAt(i);
            }
            return same;
        }

        private static boolean isLetter(char c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }

        private static boolean isWordCharacter(char c) {
            return isLetter(c) || c >= '0' && c <= '9' || c == '_';
        }

        private void text(CharSequence text, int from, int to) {
            if (inCell && from < to) {
                cell.append(text, from, to);
            }
        }

        private void tag(String text, int line) {
            switch (name) {
                case "tr" -> {
                    endRow(line);
                    if (opening) {
                        startRow(line);
                    }
                }
                case "td", "th" -> {
                    endCell();
                    if (opening) {
                        if (cells == null) {
                            startRow(line);
                        }
                        startCell(text);
                    }
                }
                case "br" -> text("<br>", 0, 4);
                // The table, its head and its body hold the rows: their tags end the row before them
                default -> endRow(line);
            }
        }

        private void startRow(int line) {
            cells = new ArrayList<>();
            rowLine = line;
        }

        private void startCell(String text) {
            cell.setLength(0);
            inCell = true;
            rowspan = 1;
            colspan = 1;
            if (attributesFrom == attributesTo) {
                return;
            }
            var spans = SPAN.matcher(text).region(attributesFrom, attributesTo);
            while (spans.find()) {
                var span = Math.max(1, Integer.parseInt(spans.group(2)));
                if ("rowspan".equalsIgnoreCase(spans.group(1))) {
                    rowspan = span;
                } else {
                    colspan = span;
                }
            }
        }

        private void endCell() {
            if (inCell && cells != null) {
                // A line break of the file inside a cell is a space, as a browser shows it
                var html = cell.indexOf("\n") < 0 ? cell.toString() : SOURCE_BREAK.matcher(cell).replaceAll(" ");
                cells.add(new TableCell(valueOf(html), rowspan, colspan));
            }
            inCell = false;
        }

        void endRow(int line) {
            endCell();
            if (cells != null) {
                rows.add(new TableRow(new TableFile.Block(rowLine, line), cells));
            }
            cells = null;
        }
    }
}
