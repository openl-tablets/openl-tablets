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
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
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
 * cell, such as {@code Rules String Greeting(Integer hour)}. The other keys are table properties, except
 * {@code merges}, which lists the merged ranges of the body in A1 notation. Values are read as written:
 * {@code 2024-01-01} stays text, and a list is joined with commas.
 *
 * <p>The body is the first GFM table or code block after the front matter. Every row of a GFM table, its first row
 * included, is a row of the body; a code block is the only body cell, as the code of a {@code Method}.
 *
 * <p>A cell is taken as written, not rendered as Markdown. A cell wrapped in backticks as a whole is unwrapped,
 * {@code \|} gives a vertical bar and {@code <br>} a line break.
 *
 * <p>Everything else in the file is documentation. A file without front matter is documentation as a whole.
 *
 * <p>A file that breaks the format in the front matter holds no table. One that breaks it in the body, or in the
 * merged ranges, keeps the table without the part that breaks it: a merged range that does not fit, the cells of a
 * row beyond those of the first row, the rows a blank line cuts off. The file tells each such problem.
 */
final class MarkdownTableParser {

    private static final Parser PARSER = Parser.builder()
            // A file of rules is trusted as a workbook is, so a table is read whatever its number of cells
            .extensions(List.of(TablesExtension.builder().maxCells(null).build()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
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
    private static final String MERGES = "merges";

    private final String text;
    private final List<String> lines;
    private final List<MarkdownProblem> problems = new ArrayList<>();

    private MarkdownTableParser(String text) {
        this.text = text;
        this.lines = Arrays.asList(LINE_SEPARATOR.split(text, -1));
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
        if (!lines.getFirst().strip().equals(FRONT_MATTER)) {
            return Optional.empty();
        }
        var end = 1;
        while (end < lines.size() && !lines.get(end).strip().equals(FRONT_MATTER)) {
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
        while (node != null && !(node instanceof TableBlock) && !(node instanceof FencedCodeBlock)) {
            node = node.getNext();
        }
        if (node == null) {
            throw error(1, "The front matter must be followed by a table or a code block");
        }
        var table = readTable(header, node);
        checkNotSplit(node);
        var separator = LINE_SEPARATOR.matcher(text);
        return Optional.of(new TableFile(text,
                lines,
                separator.find() ? separator.group() : "\n",
                table,
                end,
                keysOf(header, table),
                bodyOf(node),
                List.copyOf(problems)));
    }

    /**
     * Returns the keys of the front matter, the merged ranges among them as the table holds them: a range left out
     * stays as written until the ranges change.
     */
    private static List<TableFile.Key> keysOf(Header header, RulesTable table) {
        return header.keys()
                .stream()
                .map(key -> key.name().equals(MERGES)
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
        List<CellRangeAddress> merges = List.of();
        var mergesLine = 1;
        var keys = new ArrayList<TableFile.Key>();
        for (var tuple : mapping.getValue()) {
            var keyLine = firstLine + tuple.getKeyNode().getStartMark().getLine();
            var key = scalar(tuple.getKeyNode(), keyLine);
            var values = values(tuple.getValueNode(), keyLine);
            var value = String.join(",", values);
            switch (key) {
                case TABLE -> {
                    header = value.strip();
                    value = header;
                }
                case MERGES -> {
                    mergesLine = keyLine;
                    merges = values.stream()
                            .flatMap(range -> Arrays.stream(range.split(",")))
                            .map(String::strip)
                            .filter(range -> !range.isEmpty())
                            .flatMap(range -> range(range, keyLine).stream())
                            .toList();
                }
                default -> properties.put(key, value);
            }
            keys.add(new TableFile.Key(key, value, keyLine - 1));
        }
        if (StringUtils.isBlank(header)) {
            throw error(1, "The front matter has no 'table' key with the header of the table");
        }
        return new Header(header, properties, merges, mergesLine, keys);
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

    private RulesTable readTable(Header header, Node body) {
        var rows = body instanceof TableBlock table
                ? readRows(table, header)
                : List.of(Collections.singletonList(StringUtils.trimToNull(((FencedCodeBlock) body).getLiteral())));
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
        // A table has a source span per line: the first row, the delimiter row, then the other rows.
        var rowLines = new ArrayList<Integer>();
        rowLines.add(spans.getFirst().getLineIndex());
        spans.subList(2, spans.size()).forEach(span -> rowLines.add(span.getLineIndex()));
        return new TableFile.GfmBody(rowLines, spans.get(1).getLineIndex());
    }

    /**
     * Tells of table rows cut off from the body by a blank line: GFM reads them as a paragraph, so they are lost.
     */
    private void checkNotSplit(Node body) {
        if (body instanceof TableBlock && body.getNext() instanceof Paragraph next) {
            var lineIndex = next.getSourceSpans().getFirst().getLineIndex();
            if (lines.get(lineIndex).strip().startsWith("|")) {
                problem(lineIndex + 1, "A blank line cuts these rows off the table above. Remove it: a file holds "
                        + "one table", null);
            }
        }
    }

    /**
     * Reads the rows of a GFM table. A row keeps as many cells as the first row has, as GFM reads it.
     */
    private List<List<@Nullable String>> readRows(TableBlock table, Header header) {
        // A table has a source span per line: the first row, the delimiter row, then the other rows.
        var spans = table.getSourceSpans();
        var rows = new ArrayList<List<@Nullable String>>();
        for (var section = table.getFirstChild(); section != null; section = section.getNext()) {
            for (var row = section.getFirstChild(); row != null; row = row.getNext()) {
                var lineIndex = spans.get(rows.isEmpty() ? 0 : rows.size() + 1).getLineIndex();
                var cells = new ArrayList<@Nullable String>();
                for (var cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
                    cells.add(cellText(cell));
                }
                if (countCells(lines.get(lineIndex)) > cells.size()) {
                    problem(lineIndex + 1, "The row has more cells than the first row of the table",
                            header.cellOf(rows.size(), cells.size() - 1));
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private @Nullable String cellText(Node cell) {
        var spans = cell.getSourceSpans();
        if (spans.isEmpty()) {
            return null;
        }
        var span = spans.getFirst();
        var start = span.getColumnIndex();
        return cellText(lines.get(span.getLineIndex()).substring(start, start + span.getLength()));
    }

    /**
     * Returns the value of a cell written as {@code source}, or {@code null} for a blank cell.
     */
    static @Nullable String cellText(String source) {
        var text = unwrapCode(source.strip().replace("\\|", "|"));
        return StringUtils.trimToNull(LINE_BREAK.matcher(text).replaceAll("\n"));
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
        var text = row.strip();
        var pipes = 0;
        var lastPipe = -1;
        var i = 0;
        while (i < text.length()) {
            var c = text.charAt(i);
            if (c == '|') {
                pipes++;
                lastPipe = i;
            }
            // GFM escapes a pipe only: a backslash before anything else is a character of the cell
            i += c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '|' ? 2 : 1;
        }
        var cells = pipes + 1;
        if (text.startsWith("|")) {
            cells--;
        }
        if (lastPipe > 0 && lastPipe == text.length() - 1) {
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
     * @param mergesLine the line of the file the merged ranges are written on
     * @param keys       the keys of the front matter
     */
    private record Header(String text,
                          SequencedMap<String, String> properties,
                          List<CellRangeAddress> merges,
                          int mergesLine,
                          List<TableFile.Key> keys) {

        /**
         * Returns the cell of the sheet a cell of the body is laid out in.
         */
        CellAddress cellOf(int row, int column) {
            return new CellAddress(SheetLayout.bodyTop(properties) + row, column);
        }
    }
}
