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
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.ScalarNode;

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
 * <p>A body with merged cells is written as an HTML table, a row to a line, each merged cell spanning its rows and
 * columns (see {@link HtmlTableBuilder}); a body without any as a GFM table (see {@link GfmTableBuilder}). A merge
 * added or taken away changes the rows of the merged cell alone, as a span counts from its own cell, but the first
 * merge turns a GFM table into an HTML one, and the last one back.
 *
 * <p>A changed row of a GFM table is written with its cells padded to the widths of the delimiter row, so a table
 * aligned by hand stays aligned while a value fits. A body of one cell, read from a code block, stays a code block.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownTableWriter {

    private static final String TABLE = "table";
    private static final String PROPERTIES = "properties";
    /** The keys of the front matter the table is read from; any other key stays in the file as written. */
    private static final Set<String> TABLE_KEYS = Set.of(TABLE, PROPERTIES);
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
        if (original.header().equals(table.header()) && sameProperties(original, table)) {
            return lines.subList(0, file.frontMatterEnd() + 1);
        }
        var keys = file.keys();
        var text = new ArrayList<String>();
        // The opening line and whatever stands before the first key, such as a comment
        text.addAll(lines.subList(0, keys.isEmpty() ? file.frontMatterEnd() : keys.getFirst().line()));
        text.addAll(key(file, TABLE, table.header()));
        text.addAll(properties(file, table));
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
        var original = file.table();
        if (original.body().equals(table.body())
                && Set.copyOf(original.merges()).equals(Set.copyOf(table.merges()))) {
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
        return table(file, table);
    }

    /**
     * Returns the lines of the body of a new file after its front matter: none for a table with a header alone, a code
     * block for the code of a Method table, and a table otherwise, each after a blank line.
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
            text.addAll(table(null, table));
        }
        return text;
    }

    /**
     * Writes the body as a table, keeping the rows of the file that did not change: a row whose cells and spans are the
     * same keeps its line. A new file has none.
     */
    private static List<String> table(@Nullable TableFile file, RulesTable table) {
        var source = sourceOf(file, table);
        var rows = TableLayout.rowsOf(table);
        var kept = keptRows(TableLayout.keysOf(source.original()), TableLayout.keysOf(table));
        var builder = source.builder();
        for (var i = 0; i < rows.size(); i++) {
            if (kept[i] < 0) {
                builder.row(rows.get(i));
            } else {
                builder.written(source.lineOf(kept[i]));
            }
        }
        return builder.build();
    }

    /**
     * Returns the builder that writes the body, and the lines of the rows of the file it may keep.
     *
     * <p>A body with merged cells is an HTML table, which a GFM table cannot hold, and one without any a GFM table.
     * The rows of the file are kept while the table keeps its syntax, a GFM table its columns too; the builder then
     * keeps the lines that frame the rows: the delimiter row of a GFM table, the lines that open and close an HTML
     * one.
     */
    private static Source sourceOf(@Nullable TableFile file, RulesTable table) {
        if (!table.merges().isEmpty()) {
            if (file != null && file.body() instanceof TableFile.HtmlBody html && !html.rowLines().isEmpty()) {
                var lines = file.lines();
                var rowLines = html.rowLines();
                return new Source(HtmlTableBuilder.over(lines.subList(html.firstLine(), rowLines.getFirst()),
                        lines.subList(rowLines.getLast() + 1, html.lastLine() + 1)), file.table(), lines, rowLines);
            }
            return Source.anew(HtmlTableBuilder.of());
        }
        var width = table.width();
        if (file != null && file.body() instanceof TableFile.GfmBody gfm && file.table().width() == width) {
            return new Source(GfmTableBuilder.over(file.lines().get(gfm.delimiterLine()), width), file.table(),
                    file.lines(), gfm.rowLines());
        }
        return Source.anew(GfmTableBuilder.of(width));
    }

    /**
     * The builder that writes the body of a table, and the rows of the file it may keep.
     *
     * @param original the table as the file holds it
     * @param lines    the lines of the file
     * @param rowLines the line of each row of the file
     */
    private record Source(MarkdownTableBuilder builder,
                          RulesTable original,
                          List<String> lines,
                          List<Integer> rowLines) {

        /** Returns a source that keeps no row: the table is written anew. */
        static Source anew(MarkdownTableBuilder builder) {
            return new Source(builder, new RulesTable("", new LinkedHashMap<>(), List.of(), List.of()), List.of(),
                    List.of());
        }

        String lineOf(int row) {
            return lines.get(rowLines.get(row));
        }
    }

    private static List<String> codeBlock(TableFile.CodeBody code, @Nullable String value) {
        var indent = " ".repeat(code.indent());
        var lines = value == null
                ? List.<String>of()
                : List.of(MarkdownTableBuilder.normalized(value).split("\n", -1));
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
     * Returns, for each row of the table, the row of the original body whose lines it keeps, or -1 for a row written
     * anew. The rows above and below the change keep their lines as they stand; only the rows between are looked up,
     * in their order, so a row inserted or removed leaves the rows around it as they are.
     */
    private static <R> int[] keptRows(List<R> original, List<R> rows) {
        var top = sameAtTop(original, rows);
        var bottom = sameAtBottom(original, rows, top);
        var unchanged = rowsOf(original, top, original.size() - bottom);
        var kept = new int[rows.size()];
        var lastKept = top - 1;
        for (var i = 0; i < rows.size(); i++) {
            if (i < top) {
                kept[i] = i;
            } else if (i >= rows.size() - bottom) {
                kept[i] = original.size() - rows.size() + i;
            } else {
                kept[i] = keptRow(unchanged, rows.get(i), lastKept);
                lastKept = Math.max(lastKept, kept[i]);
            }
        }
        return kept;
    }

    /**
     * Returns how many rows at the top of the table are the same as in the original body.
     */
    private static <R> int sameAtTop(List<R> original, List<R> rows) {
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
    private static <R> int sameAtBottom(List<R> original, List<R> rows, int top) {
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
    private static <R> Map<R, Queue<Integer>> rowsOf(List<R> body, int from, int to) {
        var rows = new HashMap<R, Queue<Integer>>();
        for (var i = from; i < to; i++) {
            rows.computeIfAbsent(body.get(i), cells -> new ArrayDeque<>()).add(i);
        }
        return rows;
    }

    /**
     * Returns the original row with the same cells that comes after the last row kept, or -1 when there is none. Rows
     * are kept in their order, so a row inserted or removed leaves the rows around it as they are.
     */
    private static <R> int keptRow(Map<R, Queue<Integer>> rows, R cells, int lastKept) {
        var candidates = rows.getOrDefault(cells, new ArrayDeque<>());
        while (!candidates.isEmpty() && candidates.peek() <= lastKept) {
            candidates.remove();
        }
        return candidates.isEmpty() ? -1 : candidates.remove();
    }
}
