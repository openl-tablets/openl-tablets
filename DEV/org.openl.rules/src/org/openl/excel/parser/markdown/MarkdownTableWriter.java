package org.openl.excel.parser.markdown;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.openl.util.StringUtils;

/**
 * Writes a table back into the text of its Markdown file.
 *
 * <p>Only the front matter and the body of the table are written; the text around them stays as it is. What did not
 * change keeps its text too: a key of the front matter whose value is the same, and a row of the body whose cells
 * are the same. So a change to a cell touches its row alone, and the file stays easy to compare and to merge.
 *
 * <p>A changed row is written with its cells padded to the widths of the delimiter row, so a table aligned by hand
 * stays aligned while a value fits. A body of one cell, read from a code block, stays a code block.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownTableWriter {

    private static final String TABLE = "table";
    private static final String MERGES = "merges";
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
        var mark = !text.isEmpty() && text.charAt(0) == MarkdownReader.BYTE_ORDER_MARK ? text.substring(0, 1) : "";
        var file = MarkdownTableParser.parse(text.substring(mark.length()))
                .orElseThrow(() -> new MarkdownWriteException(NO_TABLE));
        return mark + write(file, SheetLayout.tableOf(sheet));
    }

    /**
     * Writes a table into a file of its own: the front matter, then the body as a GFM table, or as a code block for
     * the body of a Method of one cell.
     */
    static String newFile(RulesTable table) {
        var text = new ArrayList<String>();
        text.add(DELIMITER);
        text.add(rendered(TABLE, table.header()));
        table.properties().forEach((name, value) -> text.add(rendered(name, value)));
        if (!table.merges().isEmpty()) {
            text.add(rendered(MERGES, RulesTable.mergesText(table.merges())));
        }
        text.add(DELIMITER);
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
        text.addAll(lines.subList(file.frontMatterEnd() + 1, body.firstLine()));
        text.addAll(body(file, table));
        text.addAll(lines.subList(body.lastLine() + 1, lines.size()));
        return String.join(file.lineSeparator(), text);
    }

    // Front matter

    private static List<String> frontMatter(TableFile file, RulesTable table) {
        var original = file.table();
        var lines = file.lines();
        if (original.header().equals(table.header())
                && sameProperties(original, table)
                && original.merges().equals(table.merges())) {
            return lines.subList(0, file.frontMatterEnd() + 1);
        }
        var keys = file.keys();
        var text = new ArrayList<String>();
        // The opening line and whatever stands before the first key, such as a comment
        text.addAll(lines.subList(0, keys.isEmpty() ? file.frontMatterEnd() : keys.getFirst().line()));
        text.addAll(key(file, TABLE, table.header()));
        table.properties().forEach((name, value) -> text.addAll(key(file, name, value)));
        // Ranges the table is read without stay as written until the ranges change, even when no range is left
        var merges = RulesTable.mergesText(table.merges());
        if (!merges.isEmpty() || written(file, MERGES, merges).isPresent()) {
            text.addAll(key(file, MERGES, merges));
        }
        text.add(lines.get(file.frontMatterEnd()));
        return text;
    }

    private static boolean sameProperties(RulesTable original, RulesTable table) {
        return List.copyOf(original.properties().entrySet()).equals(List.copyOf(table.properties().entrySet()));
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
        var keys = file.keys();
        for (var i = 0; i < keys.size(); i++) {
            var key = keys.get(i);
            if (key.name().equals(name) && key.value().equals(value)) {
                var end = i + 1 < keys.size() ? keys.get(i + 1).line() : file.frontMatterEnd();
                return Optional.of(file.lines().subList(key.line(), end));
            }
        }
        return Optional.empty();
    }

    private static String rendered(String name, String value) {
        return MERGES.equals(name) ? name + ": [" + value + "]" : name + ": " + scalar(value);
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
        var unchanged = source == null || file == null
                ? Map.<List<@Nullable String>, Queue<Integer>>of()
                : rowsOf(file.table());
        // Cells are aligned only to the widths of a table aligned by hand
        var widths = source == null ? new int[width] : widths(delimiter, width);
        var text = new ArrayList<String>();
        var lastKept = -1;
        for (var cells : table.body()) {
            var kept = keptRow(unchanged, cells, lastKept);
            lastKept = Math.max(lastKept, kept);
            text.add(kept < 0 ? row(cells, widths) : lines.get(rowLines.get(kept)));
            if (text.size() == 1) {
                text.add(delimiter);
            }
        }
        return text;
    }

    /**
     * Returns the rows of the original body by their cells, each with the indexes of the rows holding them.
     */
    private static Map<List<@Nullable String>, Queue<Integer>> rowsOf(RulesTable table) {
        var rows = new HashMap<List<@Nullable String>, Queue<Integer>>();
        for (var i = 0; i < table.body().size(); i++) {
            rows.computeIfAbsent(table.body().get(i), cells -> new ArrayDeque<>()).add(i);
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
            widths[column] = segment.strip().equals(DELIMITER) ? 0 : Math.max(segment.length() - 2, 0);
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
