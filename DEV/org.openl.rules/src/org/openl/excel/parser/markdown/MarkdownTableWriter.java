package org.openl.excel.parser.markdown;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.SequencedMap;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.openl.util.StringUtils;

/**
 * Writes a table back into the text of its Markdown file.
 *
 * <p>Only the front matter, the description and the body of the table are written; the comments and the text after the
 * body stay as they are. What did not change keeps its text too: a key of the front matter whose value is the same,
 * the description while it is the same, and a row of the body whose cells are the same. So a change to a cell touches
 * its row alone, and the file stays easy to compare and to merge.
 *
 * <p>The description is the text between the front matter and the body, never a property of the front matter.
 *
 * <p>The merged ranges are written a range to a line under the {@code merges} key, in reading order, so a range added
 * or taken away is a line of its own. The same ranges keep their lines, in whatever order the file lists them.
 *
 * <p>A changed row is written with its cells padded to the widths of the delimiter row, so a table aligned by hand
 * stays aligned while a value fits. A body of one cell, read from a code block, stays a code block.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownTableWriter {

    private static final String TABLE = "table";
    private static final String PROPERTIES = "properties";
    private static final String MERGES = "merges";
    /** The keys of the front matter the table is read from; any other key stays in the file as written. */
    private static final Set<String> TABLE_KEYS = Set.of(TABLE, PROPERTIES, MERGES);
    /** How far a new property is indented under {@code properties}. */
    private static final String INDENT = "  ";
    private static final String DELIMITER = "---";
    private static final String METHOD = "Method ";
    /** The fence a new file writes the code of a Method in. */
    private static final TableFile.CodeBody NEW_CODE = new TableFile.CodeBody(0, 0, "```", "java", 0);
    /** The error of a file without front matter, which holds documentation alone. */
    static final String NO_TABLE = "The file holds no table to write";

    /**
     * Writes the table laid out on a sheet into the text of its file.
     *
     * @param text  the text of the file
     * @param sheet the cells of the sheet holding the table
     * @return the new text of the file
     * @throws MarkdownWriteException  when the cells or a value have no Markdown form
     * @throws MarkdownFormatException when the file does not hold a table
     */
    static String write(String text, SheetCells sheet) {
        var mark = markOf(text);
        var file = MarkdownTableParser.parse(text.substring(mark.length()))
                .orElseThrow(() -> new MarkdownWriteException(NO_TABLE));
        return write(mark, file, sheet);
    }

    /**
     * Writes the table laid out on a sheet into a file read before.
     *
     * @param mark  the byte order mark the file starts with, or an empty text
     * @param file  the file as read, without its byte order mark
     * @param sheet the cells of the sheet holding the table
     * @return the new text of the file
     * @throws MarkdownWriteException when the cells or a value have no Markdown form
     */
    static String write(String mark, TableFile file, SheetCells sheet) {
        return mark + write(file, SheetLayout.tableOf(sheet));
    }

    /**
     * Returns the byte order mark a text starts with, or an empty text when it has none.
     */
    static String markOf(String text) {
        return !text.isEmpty() && text.charAt(0) == MarkdownReader.BYTE_ORDER_MARK ? text.substring(0, 1) : "";
    }

    /**
     * Writes a table into a file of its own: the front matter, then the body as a GFM table, or as a code block for
     * the body of a Method of one cell.
     */
    static String newFile(RulesTable table) {
        var text = new ArrayList<String>();
        text.add(DELIMITER);
        text.add(rendered(TABLE, table.header()));
        var properties = frontMatterProperties(table);
        if (!properties.isEmpty()) {
            text.add(PROPERTIES + ":");
            properties.forEach((name, value) -> text.add(INDENT + rendered(name, value)));
        }
        if (!table.merges().isEmpty()) {
            text.addAll(mergesLines(table.merges()));
        }
        text.add(DELIMITER);
        var description = MarkdownDescription.linesOf(descriptionOf(table));
        if (!description.isEmpty()) {
            text.add("");
            text.addAll(description);
        }
        text.addAll(newBody(table));
        // The file ends with a line break, as a text file does
        text.add("");
        return String.join("\n", text);
    }

    /**
     * Writes a table into the text of a file.
     */
    static String write(TableFile file, RulesTable table) {
        var lines = file.lines();
        var body = file.body();
        var text = new ArrayList<>(frontMatter(file, table));
        text.addAll(beforeBody(file, table));
        text.addAll(body(file, table));
        text.addAll(lines.subList(body.lastLine() + 1, lines.size()));
        return String.join(file.lineSeparator(), text);
    }

    /**
     * Returns the properties written in the front matter: all but the description, which is the text above the body.
     */
    private static SequencedMap<String, String> frontMatterProperties(RulesTable table) {
        var properties = new LinkedHashMap<>(table.properties());
        properties.remove(MarkdownDescription.PROPERTY);
        return properties;
    }

    private static @Nullable String descriptionOf(RulesTable table) {
        return table.properties().get(MarkdownDescription.PROPERTY);
    }

    // Description

    /**
     * Returns the lines between the front matter and the body: as they are while the description is the same.
     *
     * <p>A changed description is written where the first block of the description stands, and the other blocks of
     * the description are taken away; the comments stay. A description written into a file without one goes right
     * after the front matter. A block taken away takes a blank line beside it along, so no blank lines pile up.
     */
    private static List<String> beforeBody(TableFile file, RulesTable table) {
        var lines = file.lines();
        var from = file.frontMatterEnd() + 1;
        var to = file.body().firstLine();
        if (Objects.equals(descriptionOf(file.table()), descriptionOf(table))) {
            return lines.subList(from, to);
        }
        var description = MarkdownDescription.linesOf(descriptionOf(table));
        var blocks = file.description();
        var text = new ArrayList<String>();
        if (blocks.isEmpty()) {
            text.add("");
            text.addAll(description);
            if (from == to || !lines.get(from).isBlank()) {
                text.add("");
            }
            text.addAll(lines.subList(from, to));
            return text;
        }
        var afterTakenAway = false;
        for (var line = from; line < to; line++) {
            var block = blockAt(blocks, line);
            if (block == null) {
                // The blank line after a block taken away goes with it
                if (!afterTakenAway || !lines.get(line).isBlank()) {
                    text.add(lines.get(line));
                }
                afterTakenAway = false;
            } else {
                var rewritten = block == blocks.getFirst() && !description.isEmpty();
                if (rewritten && line == block.firstLine()) {
                    text.addAll(description);
                }
                afterTakenAway = !rewritten && line == block.lastLine();
            }
        }
        // A block taken away with no blank line after it takes the one before it
        if (afterTakenAway && !text.isEmpty() && text.getLast().isBlank()) {
            text.removeLast();
        }
        return text;
    }

    private static TableFile.@Nullable Block blockAt(List<TableFile.Block> blocks, int line) {
        return blocks.stream().filter(block -> block.holds(line)).findFirst().orElse(null);
    }

    // Front matter

    private static List<String> frontMatter(TableFile file, RulesTable table) {
        var original = file.table();
        var lines = file.lines();
        if (original.header().equals(table.header())
                && sameProperties(original, table)
                && RulesTable.mergesText(original.merges()).equals(RulesTable.mergesText(table.merges()))) {
            return lines.subList(0, file.frontMatterEnd() + 1);
        }
        var keys = file.keys();
        var text = new ArrayList<String>();
        // The opening line and whatever stands before the first key, such as a comment
        text.addAll(lines.subList(0, keys.isEmpty() ? file.frontMatterEnd() : keys.getFirst().line()));
        text.addAll(key(file, TABLE, table.header()));
        text.addAll(properties(file, table));
        // Ranges the table is read without stay as written until the ranges change, even when no range is left
        var merges = RulesTable.mergesText(table.merges());
        if (!merges.isEmpty() || written(file, MERGES, merges).isPresent()) {
            text.addAll(written(file, MERGES, merges).orElseGet(() -> mergesLines(table.merges())));
        }
        keys.stream()
                .filter(key -> !TABLE_KEYS.contains(key.name()))
                .forEach(key -> text.addAll(file.lines().subList(key.line(), endOf(file, key))));
        text.add(lines.get(file.frontMatterEnd()));
        return text;
    }

    /**
     * Returns the lines of the properties: the {@code properties} key, then a line for each property under it. A
     * property keeps its lines when its value is the same, and a new one is indented as the others are.
     */
    private static List<String> properties(TableFile file, RulesTable table) {
        var properties = frontMatterProperties(table);
        if (properties.isEmpty()) {
            return List.of();
        }
        var written = file.propertyKeys();
        // Properties written on the line of the key, as {a: b}, are written again one to a line
        var block = file.keys()
                .stream()
                .filter(key -> PROPERTIES.equals(key.name())
                        && !written.isEmpty()
                        && written.getFirst().line() > key.line())
                .findFirst();
        var text = new ArrayList<String>();
        text.add(block.map(key -> file.lines().get(key.line())).orElse(PROPERTIES + ":"));
        var indent = block.map(key -> indentOf(file.lines().get(written.getFirst().line()))).orElse(INDENT);
        properties.forEach((name, value) -> text.addAll(block.flatMap(key -> writtenProperty(file, key, name, value))
                .orElseGet(() -> List.of(indent + rendered(name, value)))));
        return text;
    }

    /**
     * Returns the lines of a property as written in the file, or nothing when the file holds no such property of that
     * value.
     */
    private static Optional<List<String>> writtenProperty(TableFile file, TableFile.Key block, String name,
                                                          String value) {
        var keys = file.propertyKeys();
        var blockEnd = endOf(file, block);
        for (var i = 0; i < keys.size(); i++) {
            var key = keys.get(i);
            if (key.name().equals(name) && key.value().equals(value)) {
                var end = i + 1 < keys.size() ? keys.get(i + 1).line() : blockEnd;
                return Optional.of(file.lines().subList(key.line(), end));
            }
        }
        return Optional.empty();
    }

    private static String indentOf(String line) {
        return line.substring(0, line.length() - line.stripLeading().length());
    }

    private static boolean sameProperties(RulesTable original, RulesTable table) {
        return List.copyOf(frontMatterProperties(original).entrySet())
                .equals(List.copyOf(frontMatterProperties(table).entrySet()));
    }

    /**
     * Returns the lines of a key: as written in the file when its value is the same, written anew otherwise.
     */
    private static List<String> key(TableFile file, String name, String value) {
        return written(file, name, value).orElseGet(() -> List.of(rendered(name, value)));
    }

    /**
     * Returns the lines of a key as written in the file, or nothing when the file holds no such key of that value.
     */
    private static Optional<List<String>> written(TableFile file, String name, String value) {
        return file.keys()
                .stream()
                .filter(key -> key.name().equals(name) && key.value().equals(value))
                .findFirst()
                .map(key -> file.lines().subList(key.line(), endOf(file, key)));
    }

    /**
     * Returns the line after the lines of a key of the front matter: the line of the next key, or the closing line.
     */
    private static int endOf(TableFile file, TableFile.Key key) {
        return file.keys()
                .stream()
                .mapToInt(TableFile.Key::line)
                .filter(line -> line > key.line())
                .min()
                .orElse(file.frontMatterEnd());
    }

    private static String rendered(String name, String value) {
        return name + ": " + scalar(value);
    }

    /**
     * Returns the lines of the merged ranges: the {@code merges} key, then a range to a line under it in reading
     * order, so that a range added or taken away changes a line of its own.
     */
    private static List<String> mergesLines(List<CellRangeAddress> merges) {
        var lines = new ArrayList<String>();
        lines.add(MERGES + ":");
        RulesTable.inReadingOrder(merges).forEach(range -> lines.add(INDENT + "- " + range.formatAsString()));
        return lines;
    }

    /**
     * Writes a value as YAML: as it is when it reads back the same, in double quotes otherwise.
     */
    private static String scalar(String value) {
        try {
            // An empty value is quoted too, rather than left as trailing blanks
            var read = value.isEmpty() ? null : new Yaml().compose(new StringReader("key: " + value));
            if (read instanceof MappingNode mapping
                    && mapping.getValue().getFirst().getValueNode() instanceof ScalarNode scalar
                    && scalar.getValue().equals(value)) {
                return value;
            }
        } catch (YAMLException e) {
            // Not a plain value: quoted below
        }
        var quoted = new StringBuilder("\"");
        value.chars().forEach(c -> quoted.append(switch (c) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> Character.toString(c);
        }));
        return quoted.append('"').toString();
    }

    // Body

    private static List<String> body(TableFile file, RulesTable table) {
        var source = file.body();
        if (file.table().body().equals(table.body())) {
            return file.lines().subList(source.firstLine(), source.lastLine() + 1);
        }
        if (table.body().isEmpty()) {
            return List.of();
        }
        if (source instanceof TableFile.NoBody) {
            return newBody(table);
        }
        var body = table.body();
        if (source instanceof TableFile.CodeBody code && body.size() == 1 && body.getFirst().size() == 1
                && table.merges().isEmpty()) {
            return codeBlock(code, body.getFirst().getFirst());
        }
        return gfmTable(file, table);
    }

    /**
     * Returns the lines of the body of a new file after its front matter: none for a table with a header alone, a code
     * block for the code of a Method table, and a GFM table otherwise, each after a blank line.
     */
    private static List<String> newBody(RulesTable table) {
        var body = table.body();
        if (body.isEmpty()) {
            return List.of();
        }
        var text = new ArrayList<String>();
        text.add("");
        if (table.header().startsWith(METHOD) && body.size() == 1 && body.getFirst().size() == 1
                && table.merges().isEmpty()) {
            text.addAll(codeBlock(NEW_CODE, body.getFirst().getFirst()));
        } else {
            text.addAll(gfmTable(null, table));
        }
        return text;
    }

    private static List<String> codeBlock(TableFile.CodeBody code, @Nullable String value) {
        var indent = " ".repeat(code.indent());
        var lines = value == null ? List.<String>of() : List.of(normalized(value).split("\n", -1));
        // A fence must be longer than any run of its character in the code
        var fence = new StringBuilder(code.fence());
        while (value != null && value.contains(fence)) {
            fence.append(fence.charAt(0));
        }
        var text = new ArrayList<String>();
        text.add(indent + fence + code.info());
        lines.forEach(line -> text.add(line.isEmpty() ? line : indent + line));
        text.add(indent + fence);
        return text;
    }

    /**
     * Writes the body as a GFM table, keeping the rows of the file that did not change; a new file has none.
     */
    private static List<String> gfmTable(@Nullable TableFile file, RulesTable table) {
        var width = table.width();
        var lines = file == null ? List.<String>of() : file.lines();
        // The rows of the file are kept only while the table keeps its columns
        var source = file != null && file.body() instanceof TableFile.GfmBody gfm && file.table().width() == width
                ? gfm
                : null;
        var delimiter = source == null ? "|" + (DELIMITER + "|").repeat(width) : lines.get(source.delimiterLine());
        var rowLines = source == null ? List.<Integer>of() : source.rowLines();
        var original = source == null || file == null ? List.<List<@Nullable String>>of() : file.table().body();
        var rows = table.body();
        // The rows above and below the change keep their lines as they stand; only the rows between are looked up
        var top = sameAtTop(original, rows);
        var bottom = sameAtBottom(original, rows, top);
        var unchanged = rowsOf(original, top, original.size() - bottom);
        // Cells are aligned only to the widths of a table aligned by hand
        var widths = source == null ? new int[width] : widths(delimiter, width);
        var text = new ArrayList<String>(rows.size() + 1);
        var lastKept = top - 1;
        for (var i = 0; i < rows.size(); i++) {
            int kept;
            if (i < top) {
                kept = i;
            } else if (i >= rows.size() - bottom) {
                kept = original.size() - rows.size() + i;
            } else {
                kept = keptRow(unchanged, rows.get(i), lastKept);
                lastKept = Math.max(lastKept, kept);
            }
            text.add(kept < 0 ? row(rows.get(i), widths) : lines.get(rowLines.get(kept)));
            if (text.size() == 1) {
                text.add(delimiter);
            }
        }
        return text;
    }

    /**
     * Returns how many rows at the top of the table are the same as in the original body.
     */
    private static int sameAtTop(List<List<@Nullable String>> original, List<List<@Nullable String>> rows) {
        var top = 0;
        while (top < original.size() && top < rows.size() && original.get(top).equals(rows.get(top))) {
            top++;
        }
        return top;
    }

    /**
     * Returns how many rows at the bottom of the table are the same as in the original body, the rows the same at the
     * top left out.
     */
    private static int sameAtBottom(List<List<@Nullable String>> original, List<List<@Nullable String>> rows, int top) {
        var bottom = 0;
        while (bottom < original.size() - top && bottom < rows.size() - top
                && original.get(original.size() - 1 - bottom).equals(rows.get(rows.size() - 1 - bottom))) {
            bottom++;
        }
        return bottom;
    }

    /**
     * Returns the rows of the original body between two of them by their cells, each with the indexes of the rows
     * holding them.
     */
    private static Map<List<@Nullable String>, Queue<Integer>> rowsOf(List<List<@Nullable String>> body,
                                                                     int from,
                                                                     int to) {
        var rows = new HashMap<List<@Nullable String>, Queue<Integer>>();
        for (var i = from; i < to; i++) {
            rows.computeIfAbsent(body.get(i), cells -> new ArrayDeque<>()).add(i);
        }
        return rows;
    }

    /**
     * Returns the original row with the same cells that comes after the last row kept, or -1 when there is none. Rows
     * are kept in their order, so a row inserted or removed leaves the rows around it as they are.
     */
    private static int keptRow(Map<List<@Nullable String>, Queue<Integer>> rows,
                               List<@Nullable String> cells,
                               int lastKept) {
        var candidates = rows.getOrDefault(cells, new ArrayDeque<>());
        while (!candidates.isEmpty() && candidates.peek() <= lastKept) {
            candidates.remove();
        }
        return candidates.isEmpty() ? -1 : candidates.remove();
    }

    /**
     * Returns the width of the text of each column, read from a delimiter row such as {@code |-------|---|}.
     *
     * <p>The shortest delimiter, {@code ---}, which a new file is written with, aligns no column.
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

    private static String row(List<@Nullable String> cells, int[] widths) {
        var row = new StringBuilder("|");
        for (var column = 0; column < cells.size(); column++) {
            var text = cell(cells.get(column));
            row.append(' ').append(text).append(" ".repeat(Math.max(widths[column] - text.length(), 0))).append(" |");
        }
        return row.toString();
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
        var expected = StringUtils.trimToNull(normalized(value));
        var text = normalized(value).replace("|", "\\|").replace("\n", "<br>");
        if (Objects.equals(MarkdownTableParser.cellText(text), expected)) {
            return text;
        }
        // Wrapped in backticks, a value is taken as it is: one that starts with a backtick, for example
        var ticks = "`".repeat(longestRun(text, '`') + 1);
        var wrapped = ticks + " " + text + " " + ticks;
        if (Objects.equals(MarkdownTableParser.cellText(wrapped), expected)) {
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

    private static String normalized(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }
}
