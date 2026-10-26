package org.openl.excel.parser.markdown;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.regex.Pattern;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import org.openl.util.StringUtils;

/**
 * Reads the rules table of a Markdown file. A file holds one table at most.
 *
 * <p>The header of the table is the front matter of the file: the {@code table} key holds the text of the first table
 * cell, such as {@code Rules String Greeting(Integer hour)}. The {@code properties} key holds the table properties, a
 * name and a value on each line under it, and {@code merges} lists the merged ranges of the body in A1 notation, a
 * range to a line under it. Values are read as written: {@code NO} stays {@code NO}, and a list is joined with commas.
 * Any other key is a problem: the table is read without it.
 *
 * <p>The body is the first GFM table or code block after the front matter. Every row of a GFM table, its first row
 * included, is a row of the body; a code block is the only body cell, as the code of a {@code Method}. A file with
 * neither holds a table of a header alone, such as a {@code Datatype} that adds no field to its parent.
 *
 * <p>A cell is taken as written, not rendered as Markdown. A cell wrapped in backticks as a whole is unwrapped,
 * {@code \|} gives a vertical bar and {@code <br>} a line break. A whole number, {@code true} and {@code false}, and a
 * date, in a cell or a property, read as the values a cell of a workbook holds, and any other text as a text, see
 * {@link MarkdownValues}.
 *
 * <p>Everything else in the file is documentation. A file without front matter is documentation as a whole.
 *
 * <p>A file that breaks the format in the front matter holds no table. One that breaks it in the body, or in the
 * merged ranges, keeps the table without the part that breaks it: a merged range that does not fit, the cells of a
 * row beyond those of the first row, the rows a blank line cuts off. The file tells each such problem.
 */
final class MarkdownTableParser {

    private static final Parser PARSER = Parser.builder()
            // A table is known by its lines: its cells are cut out of the lines of the file, whatever their number
            .customBlockParserFactory(new TableLines.Factory())
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            // A cell is taken from the text of the file, so the Markdown inside it is never parsed
            .inlineParserFactory(context -> (content, node) -> {
            })
            .build();
    private static final Pattern LINE_SEPARATOR = Pattern.compile("\r\n|\r|\n");
    private static final Pattern LINE_BREAK = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    // No wider than an Excel sheet, so no row or column number overflows
    private static final Pattern A1_RANGE = Pattern.compile("[A-Z]{1,3}[1-9]\\d{0,6}(?::[A-Z]{1,3}[1-9]\\d{0,6})?");
    private static final String FRONT_MATTER = "---";
    private static final String TABLE = "table";
    private static final String PROPERTIES = "properties";
    private static final String MERGES = "merges";

    private final String text;
    private final List<String> lines;
    private final List<MarkdownProblem> problems = new ArrayList<>();

    private MarkdownTableParser(String text) {
        this.text = text;
        this.lines = linesOf(text);
    }

    /**
     * Returns the lines of a text, without their line breaks: {@code \r\n}, {@code \r} or {@code \n}.
     */
    private static List<String> linesOf(String text) {
        var lines = new ArrayList<String>();
        var start = 0;
        var i = 0;
        while (i < text.length()) {
            var c = text.charAt(i);
            var next = i + 1;
            if (c == '\n' || c == '\r') {
                lines.add(text.substring(start, i));
                // A carriage return and a line feed end one line
                next += c == '\r' && next < text.length() && text.charAt(next) == '\n' ? 1 : 0;
                start = next;
            }
            i = next;
        }
        lines.add(text.substring(start));
        return lines;
    }

    /**
     * Reads the table of a file, and where its parts stand in the text.
     *
     * @param text the text of the file
     * @return the table, or nothing for a file without front matter
     * @throws MarkdownFormatException when the file breaks the format so that it holds no table
     */
    static Optional<TableFile> parse(String text) {
        return new MarkdownTableParser(text).parse();
    }

    private Optional<TableFile> parse() {
        if (!FRONT_MATTER.equals(lines.getFirst().strip())) {
            return Optional.empty();
        }
        var end = 1;
        while (end < lines.size() && !FRONT_MATTER.equals(lines.get(end).strip())) {
            end++;
        }
        if (end == lines.size()) {
            throw error(1, "The front matter is not closed with a line of '---'");
        }
        var header = readHeader(String.join("\n", lines.subList(1, end)));

        // The front matter is blanked out rather than cut off, so the lines keep their numbers.
        var body = new ArrayList<>(lines);
        Collections.fill(body.subList(0, end + 1), "");
        var document = PARSER.parse(String.join("\n", body));
        var node = document.getFirstChild();
        while (node != null && !(node instanceof TableLines) && !(node instanceof FencedCodeBlock)) {
            node = node.getNext();
        }
        // A table with no table nor code block after its front matter has a header alone
        var table = readTable(header, node);
        if (node != null) {
            checkNotSplit(node);
        }
        var separator = LINE_SEPARATOR.matcher(text);
        return Optional.of(new TableFile(text,
                lines,
                separator.find() ? separator.group() : "\n",
                table,
                end,
                keysOf(header, table),
                header.propertyKeys(),
                node == null ? new TableFile.NoBody(end + 1) : bodyOf(node),
                List.copyOf(problems)));
    }

    /**
     * Returns the keys of the front matter, the merged ranges among them as the table holds them: a range left out
     * stays as written until the ranges change.
     */
    private static List<TableFile.Key> keysOf(Header header, RulesTable table) {
        return header.keys()
                .stream()
                .map(key -> MERGES.equals(key.name())
                        ? new TableFile.Key(MERGES, RulesTable.mergesText(table.merges()), key.line())
                        : key)
                .toList();
    }

    // Header

    private Header readHeader(String yaml) {
        // The front matter starts on the line after the first '---'
        var firstLine = 2;
        org.yaml.snakeyaml.nodes.Node root;
        try {
            root = new Yaml().compose(new StringReader(yaml));
        } catch (MarkedYAMLException e) {
            var mark = e.getProblemMark();
            throw error(mark == null ? 1 : firstLine + mark.getLine(), "Invalid YAML: " + e.getProblem());
        } catch (YAMLException e) {
            throw error(1, "Invalid YAML: " + e.getMessage());
        }
        if (!(root instanceof MappingNode mapping)) {
            throw error(1, "The front matter must be a set of 'key: value' lines");
        }

        String header = null;
        var properties = new LinkedHashMap<String, String>();
        var propertyKeys = new ArrayList<TableFile.Key>();
        List<CellRangeAddress> merges = List.of();
        var mergesLine = 1;
        var keys = new ArrayList<TableFile.Key>();
        for (var tuple : mapping.getValue()) {
            var keyLine = firstLine + tuple.getKeyNode().getStartMark().getLine();
            var key = scalar(tuple.getKeyNode(), keyLine);
            var value = "";
            switch (key) {
                case TABLE -> {
                    header = String.join(",", values(tuple.getValueNode(), keyLine)).strip();
                    value = header;
                }
                case PROPERTIES -> readProperties(tuple.getValueNode(), keyLine, firstLine, properties, propertyKeys);
                case MERGES -> {
                    var values = values(tuple.getValueNode(), keyLine);
                    value = String.join(",", values);
                    mergesLine = keyLine;
                    merges = values.stream()
                            .flatMap(range -> Arrays.stream(range.split(",")))
                            .map(String::strip)
                            .filter(range -> !range.isEmpty())
                            .flatMap(range -> range(range, keyLine).stream())
                            .toList();
                }
                // The table is read without it, and the key stays in the file as written
                default -> problem(keyLine,
                        "'%s' is not a key of the front matter: a table property is written under '%s'".formatted(key,
                                PROPERTIES),
                        null);
            }
            keys.add(new TableFile.Key(key, value, keyLine - 1));
        }
        if (header == null || header.isBlank()) {
            throw error(1, "The front matter has no 'table' key with the header of the table");
        }
        return new Header(header, properties, merges, mergesLine, keys, propertyKeys);
    }

    /**
     * Reads the properties of the table: the keys under {@code properties}, each a name and a value. A
     * {@code properties} key with nothing under it gives none.
     */
    private void readProperties(org.yaml.snakeyaml.nodes.Node node,
                                int line,
                                int firstLine,
                                SequencedMap<String, String> properties,
                                List<TableFile.Key> keys) {
        if (node instanceof ScalarNode scalar && scalar.getValue().isEmpty()) {
            return;
        }
        if (!(node instanceof MappingNode mapping)) {
            throw error(line, "The properties of the table must be a set of 'name: value' lines under 'properties'");
        }
        for (var tuple : mapping.getValue()) {
            var keyLine = firstLine + tuple.getKeyNode().getStartMark().getLine();
            var name = scalar(tuple.getKeyNode(), keyLine);
            var value = String.join(",", values(tuple.getValueNode(), keyLine));
            properties.put(name, value);
            keys.add(new TableFile.Key(name, value, keyLine - 1));
        }
    }

    private String scalar(org.yaml.snakeyaml.nodes.Node node, int line) {
        if (node instanceof ScalarNode scalar) {
            return scalar.getValue();
        }
        throw error(line, "A key of the front matter must be a single word");
    }

    private List<String> values(org.yaml.snakeyaml.nodes.Node node, int line) {
        return switch (node) {
            case ScalarNode scalar -> List.of(scalar.getValue());
            case SequenceNode sequence -> sequence.getValue().stream().map(value -> scalar(value, line)).toList();
            default -> throw error(line, "A value of the front matter must be text or a list");
        };
    }

    /**
     * Returns a merged range written in A1 notation, or nothing for one written otherwise, which is a problem.
     */
    private Optional<CellRangeAddress> range(String text, int line) {
        var range = text.toUpperCase(Locale.ROOT);
        if (!A1_RANGE.matcher(range).matches()) {
            problem(line, "'%s' is not a cell range such as A1:B2".formatted(text), null);
            return Optional.empty();
        }
        try {
            return Optional.of(CellRangeAddress.valueOf(range));
        } catch (IllegalArgumentException e) {
            problem(line, "The range '%s' must go from its top left cell to its bottom right one".formatted(text),
                    null);
            return Optional.empty();
        }
    }

    // Body

    private RulesTable readTable(Header header, @Nullable Node body) {
        var rows = switch (body) {
            case null -> List.<List<@Nullable String>>of();
            case TableLines table -> readRows(table, header);
            default -> List.of(Collections.singletonList(StringUtils.trimToNull(((FencedCodeBlock) body)
                    .getLiteral())));
        };
        var written = new RulesTable(header.text(), header.properties(), rows, header.merges());
        return new RulesTable(header.text(), header.properties(), rows, mergesThatFit(written, header));
    }

    private static TableFile.Body bodyOf(Node body) {
        var spans = body.getSourceSpans();
        if (body instanceof FencedCodeBlock code) {
            return new TableFile.CodeBody(spans.getFirst().getLineIndex(),
                    spans.getLast().getLineIndex(),
                    code.getFenceCharacter().repeat(code.getOpeningFenceLength()),
                    StringUtils.trimToEmpty(code.getInfo()),
                    code.getFenceIndent());
        }
        var table = (TableLines) body;
        return new TableFile.GfmBody(table.rowLines(), table.delimiterLine());
    }

    /**
     * Tells of table rows cut off from the body by a blank line: GFM reads them as a paragraph, so they are lost.
     */
    private void checkNotSplit(Node body) {
        if (body instanceof TableLines && body.getNext() instanceof Paragraph next) {
            var lineIndex = next.getSourceSpans().getFirst().getLineIndex();
            if (lines.get(lineIndex).strip().startsWith("|")) {
                problem(lineIndex + 1, """
                        A blank line cuts these rows off the table above. Remove it: a file holds \
                        one table""", null);
            }
        }
    }

    /**
     * Reads the rows of a GFM table. A row keeps as many cells as the first row has, as GFM reads it.
     */
    private List<List<@Nullable String>> readRows(TableLines table, Header header) {
        var rowLines = table.rowLines();
        var width = TableLines.cellBounds(lines.get(rowLines.getFirst())).length / 2;
        var rows = new ArrayList<List<@Nullable String>>(rowLines.size());
        for (var lineIndex : rowLines) {
            var line = lines.get(lineIndex);
            var bounds = TableLines.cellBounds(line);
            var cells = new ArrayList<@Nullable String>(width);
            for (var column = 0; column < width; column++) {
                cells.add(cellText(line, bounds, column));
            }
            if (countCells(line) > width) {
                problem(lineIndex + 1, "The row has more cells than the first row of the table",
                        header.cellOf(rows.size(), width - 1));
            }
            rows.add(cells);
        }
        return rows;
    }

    /**
     * Returns the value of a cell of a row, or {@code null} for a blank cell or one the row does not have.
     *
     * @param bounds where the cells of the row stand, see {@link TableLines#cellBounds}
     */
    private static @Nullable String cellText(String row, int[] bounds, int column) {
        if (2 * column >= bounds.length) {
            return null;
        }
        var from = bounds[2 * column];
        var to = bounds[2 * column + 1];
        while (from < to && Character.isWhitespace(row.charAt(from))) {
            from++;
        }
        while (to > from && Character.isWhitespace(row.charAt(to - 1))) {
            to--;
        }
        return from == to ? null : cellText(row.substring(from, to));
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

    /**
     * Counts the cells of a table row the way GFM splits it, before GFM drops the cells past the first row's.
     */
    static int countCells(String row) {
        var start = 0;
        var end = row.length();
        while (start < end && Character.isWhitespace(row.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(row.charAt(end - 1))) {
            end--;
        }
        var pipes = 0;
        var lastPipe = -1;
        var i = start;
        while (i < end) {
            var c = row.charAt(i);
            if (c == '|') {
                pipes++;
                lastPipe = i;
            }
            // GFM escapes a pipe only: a backslash before anything else is a character of the cell
            i += c == '\\' && i + 1 < end && row.charAt(i + 1) == '|' ? 2 : 1;
        }
        var cells = pipes + 1;
        if (start < end && row.charAt(start) == '|') {
            cells--;
        }
        if (lastPipe > start && lastPipe == end - 1) {
            cells--;
        }
        return cells;
    }

    /**
     * Returns the merged ranges of the table that fit it. A range outside the table, over a range before it or over
     * a value is a problem, and the table is read without it.
     */
    private List<CellRangeAddress> mergesThatFit(RulesTable table, Header header) {
        var fit = new ArrayList<CellRangeAddress>();
        for (var range : table.merges()) {
            if (fits(table, range, fit, header)) {
                fit.add(range);
            }
        }
        return fit;
    }

    private boolean fits(RulesTable table, CellRangeAddress range, List<CellRangeAddress> before, Header header) {
        var line = header.mergesLine();
        var height = table.body().size();
        var width = table.width();
        if (range.getLastRow() >= height || range.getLastColumn() >= width) {
            // The problem shows in the first cell of the range, when the table has that cell
            var first = range.getFirstRow() < height && range.getFirstColumn() < width
                    ? header.cellOf(range.getFirstRow(), range.getFirstColumn())
                    : null;
            problem(line, "The merged range %s is outside the table".formatted(range.formatAsString()), first);
            return false;
        }
        var overlapped = before.stream().filter(range::intersects).findFirst();
        if (overlapped.isPresent()) {
            var other = overlapped.get();
            problem(line, "The merged ranges %s and %s overlap".formatted(other.formatAsString(),
                            range.formatAsString()),
                    header.cellOf(Math.max(range.getFirstRow(), other.getFirstRow()),
                            Math.max(range.getFirstColumn(), other.getFirstColumn())));
            return false;
        }
        var covered = coveredValue(table.body(), range);
        if (covered != null) {
            problem(line, "The cell %s has a value, but the merged range %s covers it".formatted(
                    covered.formatAsString(), range.formatAsString()),
                    header.cellOf(covered.getRow(), covered.getColumn()));
            return false;
        }
        return true;
    }

    /**
     * Returns the first cell of the body that holds a value and that the range covers but does not start with.
     */
    private static @Nullable CellAddress coveredValue(List<List<@Nullable String>> rows, CellRangeAddress range) {
        for (var row = range.getFirstRow(); row <= range.getLastRow(); row++) {
            for (var column = range.getFirstColumn(); column <= range.getLastColumn(); column++) {
                var covered = row != range.getFirstRow() || column != range.getFirstColumn();
                if (covered && rows.get(row).get(column) != null) {
                    return new CellAddress(row, column);
                }
            }
        }
        return null;
    }

    private void problem(int line, String problem, @Nullable CellAddress cell) {
        problems.add(new MarkdownProblem(line, problem, cell));
    }

    private MarkdownFormatException error(int line, String message) {
        return new MarkdownFormatException(line, message);
    }

    /**
     * The header of the table, read from the front matter.
     *
     * @param mergesLine   the line of the file the merged ranges are written on
     * @param keys         the keys of the front matter
     * @param propertyKeys the keys under {@code properties}
     */
    private record Header(String text,
                          SequencedMap<String, String> properties,
                          List<CellRangeAddress> merges,
                          int mergesLine,
                          List<TableFile.Key> keys,
                          List<TableFile.Key> propertyKeys) {

        /**
         * Returns the cell of the sheet a cell of the body is laid out in.
         */
        CellAddress cellOf(int row, int column) {
            return new CellAddress(SheetLayout.bodyTop(properties) + row, column);
        }
    }
}
