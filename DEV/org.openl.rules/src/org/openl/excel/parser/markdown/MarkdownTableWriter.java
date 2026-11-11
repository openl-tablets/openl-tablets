package org.openl.excel.parser.markdown;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
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
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;
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
 * <p>The text after the body is never read into the table: it follows the table written as the file holds it, so
 * however long it is, it costs a copy and no memory while the table is edited.
 *
 * <p>The description is the text between the front matter and the body, never a property of the front matter.
 *
 * <p>The merged ranges are written a range to a line under the {@code merges} key, in reading order, so a range added
 * or taken away is a line of its own. The same ranges keep their lines, in whatever order the file lists them.
 *
 * <p>A changed row is written with its cells padded to the widths of the delimiter row, so a table aligned by hand
 * stays aligned while a value fits (see {@link GfmTableBuilder}). A body of one cell, read from a code block, stays a
 * code block.
 *
 * <p>A Spreadsheet is written in parts: a row that holds a caption, merged across the table, is written as a paragraph
 * between two GFM tables rather than as a merged range, and the next table repeats the first row (see
 * {@link SpreadsheetParts}).
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
        var withoutMark = text.substring(mark.length());
        var file = MarkdownTableParser.parse(withoutMark).orElseThrow(() -> new MarkdownWriteException(NO_TABLE));
        return head(mark, file, sheet) + file.tailOf(withoutMark);
    }

    /**
     * Writes the table laid out on a sheet into a file read before, up to the end of the table. The text after the
     * table follows as the file holds it, from its line break on (see {@link TableFile#tailOf}).
     *
     * @param mark  the byte order mark the file starts with, or an empty text
     * @param file  the file as read, without its byte order mark
     * @param sheet the cells of the sheet holding the table
     * @return the new text of the file up to the end of the table
     * @throws MarkdownWriteException when the cells or a value have no Markdown form
     */
    static String head(String mark, TableFile file, SheetCells sheet) {
        return mark + head(file, SheetLayout.tableOf(sheet));
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
        var merges = mergesToWrite(table);
        if (!merges.isEmpty()) {
            text.addAll(mergesLines(merges));
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
     * Writes a table into the text of a file up to the end of the table.
     */
    private static String head(TableFile file, RulesTable table) {
        var text = new ArrayList<>(frontMatter(file, table));
        text.addAll(beforeBody(file, table));
        text.addAll(body(file, table));
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

    /**
     * Returns the merged ranges written under {@code merges}: all but those of the captions, which are paragraphs.
     */
    private static List<CellRangeAddress> mergesToWrite(RulesTable table) {
        return SpreadsheetParts.mergesBesides(table.merges(), SpreadsheetParts.captionRows(table));
    }

    /**
     * Returns the merged ranges of the table of a file as written under {@code merges}.
     */
    private static List<CellRangeAddress> mergesWritten(TableFile file) {
        return SpreadsheetParts.mergesBesides(file.table().merges(), captionRowsOf(file));
    }

    /**
     * Returns the rows of the table of a file written as captions.
     */
    private static BitSet captionRowsOf(TableFile file) {
        var rows = new BitSet();
        if (file.body() instanceof TableFile.GfmBody gfm) {
            gfm.captions().keySet().forEach(rows::set);
        }
        return rows;
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
        return blocks.isEmpty()
                ? descriptionAdded(lines.subList(from, to), description)
                : descriptionReplaced(lines, from, to, blocks, description);
    }

    /**
     * Returns the lines between the front matter and the body of a file without a description, the description
     * written before them.
     */
    private static List<String> descriptionAdded(List<String> lines, List<String> description) {
        var text = new ArrayList<String>();
        text.add("");
        text.addAll(description);
        if (lines.isEmpty() || !lines.getFirst().isBlank()) {
            text.add("");
        }
        text.addAll(lines);
        return text;
    }

    /**
     * Returns the lines between the front matter and the body, the description written over its first block and its
     * other blocks taken away.
     */
    private static List<String> descriptionReplaced(List<String> lines,
                                                    int from,
                                                    int to,
                                                    List<TableFile.Block> blocks,
                                                    List<String> description) {
        var text = new ArrayList<String>();
        var afterTakenAway = false;
        for (var line = from; line < to; line++) {
            var block = blockAt(blocks, line);
            if (block == null) {
                // The blank line after a block taken away goes with it
                if (!(afterTakenAway && lines.get(line).isBlank())) {
                    text.add(lines.get(line));
                }
                afterTakenAway = false;
            } else if (block != blocks.getFirst() || description.isEmpty()) {
                afterTakenAway = line == block.lastLine();
            } else {
                if (line == block.firstLine()) {
                    text.addAll(description);
                }
                afterTakenAway = false;
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
        var toWrite = mergesToWrite(table);
        var merges = RulesTable.mergesText(toWrite);
        if (original.header().equals(table.header())
                && sameProperties(original, table)
                && RulesTable.mergesText(mergesWritten(file)).equals(merges)) {
            return lines.subList(0, file.frontMatterEnd() + 1);
        }
        var keys = file.keys();
        var text = new ArrayList<String>();
        // The opening line and whatever stands before the first key, such as a comment
        text.addAll(lines.subList(0, keys.isEmpty() ? file.frontMatterEnd() : keys.getFirst().line()));
        text.addAll(key(file, TABLE, table.header()));
        text.addAll(properties(file, table));
        // Ranges the table is read without stay as written until the ranges change, even when no range is left
        if (!merges.isEmpty() || written(file, MERGES, merges).isPresent()) {
            text.addAll(written(file, MERGES, merges)
                    .orElseGet(() -> withComments(file, MERGES, mergesLines(toWrite))));
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
                .orElseGet(() -> {
                    var lines = new ArrayList<String>();
                    lines.add(indent + rendered(name, value));
                    block.ifPresent(key -> lines.addAll(commentsAfter(propertyLines(file, key, name))));
                    return lines;
                })));
        return text;
    }

    /**
     * Returns the lines of a property as written in the file, whatever its value, or none for a property the file
     * does not hold.
     */
    private static List<String> propertyLines(TableFile file, TableFile.Key block, String name) {
        var keys = file.propertyKeys();
        for (var i = 0; i < keys.size(); i++) {
            if (keys.get(i).name().equals(name)) {
                var end = i + 1 < keys.size() ? keys.get(i + 1).line() : endOf(file, block);
                return file.lines().subList(keys.get(i).line(), end);
            }
        }
        return List.of();
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
        return written(file, name, value).orElseGet(() -> withComments(file, name, List.of(rendered(name, value))));
    }

    /**
     * Returns the lines of a key written anew, followed by the comments and the blank lines the key was written with
     * after its value: a comment after a key stays when the value of the key changes.
     */
    private static List<String> withComments(TableFile file, String name, List<String> rendered) {
        var text = new ArrayList<>(rendered);
        file.keys()
                .stream()
                .filter(key -> key.name().equals(name))
                .findFirst()
                .ifPresent(key -> text.addAll(commentsAfter(file.lines().subList(key.line(), endOf(file, key)))));
        return text;
    }

    /** Returns the comment and blank lines the lines of a key end with, its first line left out. */
    private static List<String> commentsAfter(List<String> lines) {
        var start = lines.size();
        while (start > 1 && (lines.get(start - 1).isBlank() || lines.get(start - 1).strip().startsWith("#"))) {
            start--;
        }
        return lines.subList(start, lines.size());
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
        if (file.table().body().equals(table.body())
                && captionRowsOf(file).equals(SpreadsheetParts.captionRows(table))) {
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
     * Writes the body as GFM tables, keeping the rows of the file that did not change; a new file has none. A caption
     * of a Spreadsheet ends a table: it is written as a paragraph, and the next table repeats the first row.
     */
    private static List<String> gfmTable(@Nullable TableFile file, RulesTable table) {
        var width = table.width();
        // The rows of the file are kept only while the table keeps its columns
        var source = file != null && file.body() instanceof TableFile.GfmBody gfm && file.table().width() == width
                ? Source.of(file.lines(), gfm)
                : null;
        var original = source == null || file == null ? List.<List<@Nullable String>>of() : file.table().body();
        var rows = table.body();
        var kept = keptRows(original, rows);
        var captions = SpreadsheetParts.captionRows(table);
        var parts = new GfmParts(source, width, rows.getFirst(), kept[0] == 0);
        for (var i = 0; i < rows.size(); i++) {
            var written = kept[i] >= 0 && source != null ? source.row(kept[i], captions.get(i)) : null;
            if (captions.get(i)) {
                parts.caption(rows.get(i).getFirst(), written);
            } else {
                parts.row(rows.get(i), written);
            }
        }
        return parts.build();
    }

    /**
     * Writes the GFM tables of a body row by row. A caption ends a table, and the next table stands after a blank line
     * and starts with the first row of the body, as the table of the file writes it while that row is the same.
     */
    @RequiredArgsConstructor
    private static final class GfmParts {

        private final @Nullable Source source;
        private final int width;
        private final List<@Nullable String> firstRow;
        private final boolean firstRowKept;
        private final List<String> text = new ArrayList<>();
        private @Nullable GfmTableBuilder builder;
        private int parts;

        /**
         * Writes a caption: the lines of the file when it keeps them, else the caption itself.
         */
        void caption(@Nullable String caption, @Nullable List<String> written) {
            end();
            text.add("");
            text.addAll(written == null ? List.of(Objects.requireNonNull(caption)) : written);
        }

        /**
         * Writes a row: the line of the file when it keeps it, else the cells of the row.
         */
        void row(List<@Nullable String> row, @Nullable List<String> written) {
            var table = builder == null ? start() : builder;
            if (written == null) {
                table.row(row);
            } else {
                table.written(written.getFirst());
            }
        }

        /**
         * Returns the lines of the tables. A body ending with a caption ends with a table of its first row alone, so
         * the caption reads back.
         */
        List<String> build() {
            if (builder == null) {
                start();
            }
            end();
            return text;
        }

        private GfmTableBuilder start() {
            var part = parts++;
            var table = source == null ? GfmTableBuilder.of(width) : source.builder(part, width);
            if (part > 0) {
                text.add("");
                var written = firstRowKept && source != null ? source.firstRow(part) : null;
                if (written == null) {
                    table.row(firstRow);
                } else {
                    table.written(written);
                }
            }
            builder = table;
            return table;
        }

        private void end() {
            if (builder != null) {
                text.addAll(builder.build());
                builder = null;
            }
        }
    }

    /**
     * Returns for each row of the table the row of the original body it keeps the lines of, or -1 for a row written
     * anew.
     */
    private static int[] keptRows(List<List<@Nullable String>> original, List<List<@Nullable String>> rows) {
        // The rows above and below the change keep their lines as they stand; only the rows between are looked up
        var top = sameAtTop(original, rows);
        var bottom = sameAtBottom(original, rows, top);
        var unchanged = rowsOf(original, top, original.size() - bottom);
        var lastKept = top - 1;
        var kept = new int[rows.size()];
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
     * The GFM tables of a file a body is written over.
     *
     * @param lines    the lines of the file
     * @param body     the tables of the body in the file
     * @param rowLines the line of each row of the body
     * @param captions the caption blocks by the rows of the body they are
     */
    private record Source(List<String> lines,
                          TableFile.GfmBody body,
                          List<Integer> rowLines,
                          Map<Integer, TableFile.Block> captions) {

        static Source of(List<String> lines, TableFile.GfmBody body) {
            return new Source(lines, body, body.rowLines(), body.captions());
        }

        /**
         * Returns the lines of a row of the original body, or nothing when the row is a caption and the row written is
         * not, or the other way round.
         */
        @Nullable
        List<String> row(int original, boolean caption) {
            var block = captions.get(original);
            if (caption != (block != null)) {
                return null;
            }
            return block == null
                    ? List.of(lines.get(rowLines.get(original)))
                    : lines.subList(block.firstLine(), block.lastLine() + 1);
        }

        /**
         * Returns a builder of a table of the body, aligned as the table of the file at that place, or as the first
         * table of the file when it has fewer.
         */
        GfmTableBuilder builder(int part, int width) {
            var parts = body.parts();
            return GfmTableBuilder.over(lines.get(parts.get(Math.min(part, parts.size() - 1)).delimiterLine()), width);
        }

        /**
         * Returns the first row of a table of the file as written, or nothing when the file has fewer tables.
         */
        @Nullable
        String firstRow(int part) {
            var parts = body.parts();
            return part < parts.size() ? lines.get(parts.get(part).rowLines().getFirst()) : null;
        }
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
}
