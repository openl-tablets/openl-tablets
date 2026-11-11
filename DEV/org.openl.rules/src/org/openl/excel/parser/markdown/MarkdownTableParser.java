package org.openl.excel.parser.markdown;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HtmlBlock;
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

import org.openl.rules.lang.xls.XlsHelper;
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
 * <p>The text between the front matter and the body is the description of the table, its {@code description}
 * property; a quote and an HTML comment there are comments, and no part of it (see {@link MarkdownDescription}). A
 * {@code description} under {@code properties} is a problem: the table is read without it.
 *
 * <p>The body is the first GFM table or code block after the front matter. Every row of a GFM table, its first row
 * included, is a row of the body (see {@link GfmTableReader}); a code block is the only body cell, as the code of a
 * {@code Method}. A file with neither holds a table of a header alone, such as a {@code Datatype} that adds no field to
 * its parent; the text after its front matter is its description.
 *
 * <p>A Spreadsheet goes on in the GFM tables after the first one that repeat its first row, the paragraphs between
 * them being captions: each caption is a row of the body, its text in the first cell, merged across the table (see
 * {@link SpreadsheetParts}). The merged ranges of {@code merges} count the rows of the body so put together.
 *
 * <p>A cell is taken as written, not rendered as Markdown. A cell wrapped in backticks as a whole is unwrapped,
 * {@code \|} gives a vertical bar and {@code <br>} a line break. A whole number, {@code true} and {@code false}, and a
 * date, in a cell or a property, read as the values a cell of a workbook holds, and any other text as a text, see
 * {@link MarkdownValues}.
 *
 * <p>The text after the body is documentation. A file without front matter is documentation as a whole, and so is one
 * whose front matter has none of the keys {@code table}, {@code properties} and {@code merges}, such as a page of a
 * site.
 *
 * <p>A file that breaks the format in the front matter holds no table. One that breaks it in the body, or in the
 * merged ranges, keeps the table without the part that breaks it: a merged range that does not fit, any merged range
 * of a Data, Test or Run table, whose record is one row, the cells of a row beyond those of the first row, the rows a
 * blank line cuts off, a table after a Spreadsheet that does not repeat its first row. The file tells each such
 * problem.
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
    // No wider than an Excel sheet, so no row or column number overflows
    private static final Pattern A1_RANGE = Pattern.compile("[A-Z]{1,3}[1-9]\\d{0,6}(?::[A-Z]{1,3}[1-9]\\d{0,6})?");
    private static final String FRONT_MATTER = "---";
    private static final String TABLE = "table";
    private static final String PROPERTIES = "properties";
    private static final String MERGES = "merges";
    static final String NO_MERGES_IN_RECORDS =
            "A Data, Test or Run table holds no merged cells: index each element, such as 'drivers[0].name'";

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
     * @return the table, or nothing for a file of documentation
     * @throws MarkdownFormatException when the file breaks the format so that it holds no table
     */
    static Optional<TableFile> parse(String text) {
        return new MarkdownTableParser(text).parse();
    }

    /** Whether a line opens or closes the front matter of a file, as the first line of a file of a table does. */
    static boolean opensFrontMatter(String line) {
        return FRONT_MATTER.equals(line.strip());
    }

    /**
     * Whether a line of the front matter is a key of the front matter of a table: {@code table}, {@code properties} or
     * {@code merges}. A front matter with none of them sets up something else, such as a page of a site.
     */
    static boolean namesATableKey(String line) {
        return Stream.of(TABLE, PROPERTIES, MERGES)
                .anyMatch(key -> line.startsWith(key) && line.substring(key.length()).stripLeading().startsWith(":"));
    }

    private Optional<TableFile> parse() {
        if (!opensFrontMatter(lines.getFirst())) {
            return Optional.empty();
        }
        var end = 1;
        while (end < lines.size() && !FRONT_MATTER.equals(lines.get(end).strip())) {
            end++;
        }
        if (end == lines.size()) {
            throw error(1, "The front matter is not closed with a line of '---'");
        }
        // A front matter with no key of a table, such as the title of a page of a site, makes a page of documentation
        if (lines.subList(1, end).stream().noneMatch(MarkdownTableParser::namesATableKey)) {
            return Optional.empty();
        }
        var written = readHeader(String.join("\n", lines.subList(1, end)));

        // The front matter is blanked out rather than cut off, so the lines keep their numbers.
        var body = new ArrayList<>(lines);
        Collections.fill(body.subList(0, end + 1), "");
        var document = PARSER.parse(String.join("\n", body));
        var node = document.getFirstChild();
        var description = new ArrayList<TableFile.Block>();
        while (node != null && !(node instanceof TableLines) && !(node instanceof FencedCodeBlock)) {
            if (!isComment(node)) {
                description.add(blockOf(node));
            }
            node = node.getNext();
        }
        var header = described(written, MarkdownDescription.of(lines, description));
        var gfm = node instanceof TableLines first ? new TableFile.GfmBody(partsOf(first, header)) : null;
        // A table with no table nor code block after its front matter has a header alone
        var table = readTable(header, node, gfm);
        var separator = LINE_SEPARATOR.matcher(text);
        var afterDescription = description.isEmpty() ? end + 1 : description.getLast().lastLine() + 1;
        var tableBody = gfm != null ? gfm : bodyOf(node, afterDescription);
        // The text after the body is not kept: a write takes it from the file
        return Optional.of(new TableFile(List.copyOf(lines.subList(0, tableBody.lastLine() + 1)),
                separator.find() ? separator.group() : "\n",
                table,
                end,
                keysOf(header, table, gfm),
                header.propertyKeys(),
                List.copyOf(description),
                tableBody,
                List.copyOf(problems)));
    }

    /**
     * Tells a comment: a quote, or an HTML comment, which a preview does not show.
     */
    private static boolean isComment(Node node) {
        return node instanceof BlockQuote || node instanceof HtmlBlock html && html.getLiteral().startsWith("<!--");
    }

    private static TableFile.Block blockOf(Node node) {
        var spans = node.getSourceSpans();
        return new TableFile.Block(spans.getFirst().getLineIndex(), spans.getLast().getLineIndex());
    }

    /**
     * Returns the header with the description of the table first among its properties, as the description is the
     * first thing written about the table.
     */
    private static Header described(Header header, @Nullable String description) {
        if (description == null) {
            return header;
        }
        var properties = new LinkedHashMap<String, String>();
        properties.put(MarkdownDescription.PROPERTY, description);
        properties.putAll(header.properties());
        return new Header(header.text(), properties, header.merges(), header.mergesLine(), header.keys(),
                header.propertyKeys());
    }

    /**
     * Returns the keys of the front matter, the merged ranges among them as the table holds them: a range left out
     * stays as written until the ranges change. The ranges of the captions are no part of them.
     */
    private static List<TableFile.Key> keysOf(Header header, RulesTable table, TableFile.@Nullable GfmBody gfm) {
        var captions = new BitSet();
        if (gfm != null) {
            gfm.captions().keySet().forEach(captions::set);
        }
        var merges = RulesTable.mergesText(SpreadsheetParts.mergesBesides(table.merges(), captions));
        return header.keys()
                .stream()
                .map(key -> MERGES.equals(key.name()) ? new TableFile.Key(MERGES, merges, key.line()) : key)
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
            if (MarkdownDescription.PROPERTY.equals(name)) {
                // The table is read without it
                problem(keyLine, """
                        The description of a table is the text between the front matter and the table, not a \
                        property under '%s'""".formatted(PROPERTIES), null);
            } else {
                properties.put(name, value);
            }
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

    private RulesTable readTable(Header header, @Nullable Node body, TableFile.@Nullable GfmBody gfm) {
        List<List<@Nullable String>> rows;
        if (gfm != null) {
            rows = readRows(header, new GfmTableReader(lines, gfm.rowLines(), captionTexts(gfm)));
        } else if (body instanceof FencedCodeBlock code) {
            rows = List.of(Collections.singletonList(StringUtils.trimToNull(code.getLiteral())));
        } else {
            rows = List.of();
        }
        var width = rows.isEmpty() ? 0 : rows.getFirst().size();
        var captions = gfm == null || width < 2
                ? List.<CellRangeAddress>of()
                : gfm.captions().keySet().stream().sorted().map(row -> SpreadsheetParts.rangeOf(row, width)).toList();
        return new RulesTable(header.text(), header.properties(), rows, mergesThatFit(rows, header, captions));
    }

    /**
     * Returns the text of each caption of a body by the first line of the caption.
     */
    private Map<Integer, String> captionTexts(TableFile.GfmBody gfm) {
        var texts = new HashMap<Integer, String>();
        gfm.captions()
                .values()
                .forEach(block -> texts.put(block.firstLine(), SpreadsheetParts.captionOf(lines, block)));
        return texts;
    }

    /**
     * Returns the GFM tables of the body: the first one and, for a Spreadsheet, each table after it that repeats its
     * first row, with the paragraphs above it as its captions. A paragraph after the last such table is documentation.
     */
    private List<TableFile.Part> partsOf(TableLines first, Header header) {
        var parts = new ArrayList<TableFile.Part>();
        parts.add(new TableFile.Part(List.of(), first.rowLines(), first.delimiterLine()));
        var last = first;
        if (SpreadsheetParts.isSpreadsheet(header.text())) {
            var columns = GfmTableReader.cellsOf(lines.get(first.rowLines().getFirst()));
            var captions = new ArrayList<TableFile.Block>();
            var node = first.getNext();
            while (isCaption(node) || node instanceof TableLines next && repeats(next, columns)) {
                if (node instanceof TableLines table) {
                    parts.add(new TableFile.Part(List.copyOf(captions), table.rowLines(), table.delimiterLine()));
                    captions.clear();
                    last = table;
                } else {
                    captions.add(blockOf(node));
                }
                node = node.getNext();
            }
            if (node instanceof TableLines table) {
                problem(table.rowLines().getFirst() + 1,
                        "The table does not repeat the first row of the Spreadsheet above, so it is no part of it",
                        null);
            }
        }
        checkNotSplit(last);
        return parts;
    }

    private boolean isCaption(@Nullable Node node) {
        return node instanceof Paragraph
                && SpreadsheetParts.isCaption(SpreadsheetParts.captionOf(lines, blockOf(node)));
    }

    private boolean repeats(TableLines table, List<@Nullable String> columns) {
        return GfmTableReader.cellsOf(lines.get(table.rowLines().getFirst())).equals(columns);
    }

    private static TableFile.Body bodyOf(@Nullable Node body, int afterDescription) {
        if (!(body instanceof FencedCodeBlock code)) {
            return new TableFile.NoBody(afterDescription);
        }
        var spans = body.getSourceSpans();
        return new TableFile.CodeBody(spans.getFirst().getLineIndex(),
                spans.getLast().getLineIndex(),
                code.getFenceCharacter().repeat(code.getOpeningFenceLength()),
                StringUtils.trimToEmpty(code.getInfo()),
                code.getFenceIndent());
    }

    /**
     * Tells of table rows cut off from the body by a blank line: GFM reads them as a paragraph, so they are lost.
     */
    private void checkNotSplit(TableLines body) {
        if (body.getNext() instanceof Paragraph next) {
            var lineIndex = next.getSourceSpans().getFirst().getLineIndex();
            if (lines.get(lineIndex).strip().startsWith("|")) {
                problem(lineIndex + 1, """
                        A blank line cuts these rows off the table above. Remove it: a file holds \
                        one table""", null);
            }
        }
    }

    /**
     * Reads the rows of a table. A row keeps as many cells as the first row has, as GFM reads it: a cell past them is
     * a problem, and the table is read without it.
     */
    private List<List<@Nullable String>> readRows(Header header, MarkdownTableReader reader) {
        var written = reader.getRows();
        var width = written.getFirst().cells().size();
        var rows = new ArrayList<List<@Nullable String>>(written.size());
        for (var row : written) {
            var cells = row.cells();
            if (cells.size() > width) {
                problem(row.lines().firstLine() + 1, "The row has more cells than the first row of the table",
                        header.cellOf(rows.size(), width - 1));
            }
            rows.add(cells.size() == width ? cells : fitted(cells, width));
        }
        return rows;
    }

    /**
     * Returns the cells of a row as wide as the table: the cells past its width are cut off, and the cells a row does
     * not have are blank.
     */
    private static List<@Nullable String> fitted(List<@Nullable String> cells, int width) {
        var fitted = new ArrayList<@Nullable String>(Collections.nCopies(width, null));
        for (var column = 0; column < Math.min(width, cells.size()); column++) {
            fitted.set(column, cells.get(column));
        }
        return fitted;
    }

    /**
     * Returns the merged ranges of the table that fit it: the ranges of the captions, then those of {@code merges}. A
     * range outside the table, over a range before it or over a value is a problem, and the table is read without it.
     * A Data, Test or Run table holds no merged range: its {@code merges} are a problem, and the table is read without
     * them.
     */
    private List<CellRangeAddress> mergesThatFit(List<List<@Nullable String>> body,
                                                 Header header,
                                                 List<CellRangeAddress> captions) {
        var fit = new ArrayList<CellRangeAddress>();
        // The columns the ranges that fit take in each row, so a range is checked against its own cells alone
        var taken = new HashMap<Integer, BitSet>();
        var width = body.isEmpty() ? 0 : body.getFirst().size();
        captions.forEach(range -> take(fit, taken, range, width));
        if (!header.merges().isEmpty() && XlsHelper.isRecordsTableHeader(header.text())) {
            problem(header.mergesLine(), NO_MERGES_IN_RECORDS, null);
            return fit;
        }
        for (var range : header.merges()) {
            if (fits(body, width, range, taken, fit, header)) {
                take(fit, taken, range, width);
            }
        }
        return fit;
    }

    private static void take(List<CellRangeAddress> fit, Map<Integer, BitSet> taken, CellRangeAddress range,
                             int width) {
        fit.add(range);
        for (var row = range.getFirstRow(); row <= range.getLastRow(); row++) {
            taken.computeIfAbsent(row, r -> new BitSet(width)).set(range.getFirstColumn(), range.getLastColumn() + 1);
        }
    }

    private boolean fits(List<List<@Nullable String>> body,
                         int width,
                         CellRangeAddress range,
                         Map<Integer, BitSet> taken,
                         List<CellRangeAddress> before,
                         Header header) {
        var line = header.mergesLine();
        var height = body.size();
        if (range.getLastRow() >= height || range.getLastColumn() >= width) {
            // The problem shows in the first cell of the range, when the table has that cell
            var first = range.getFirstRow() < height && range.getFirstColumn() < width
                    ? header.cellOf(range.getFirstRow(), range.getFirstColumn())
                    : null;
            problem(line, "The merged range %s is outside the table".formatted(range.formatAsString()), first);
            return false;
        }
        if (overlapsTaken(range, taken)) {
            var other = before.stream().filter(range::intersects).findFirst().orElseThrow();
            problem(line, "The merged ranges %s and %s overlap".formatted(other.formatAsString(),
                            range.formatAsString()),
                    header.cellOf(Math.max(range.getFirstRow(), other.getFirstRow()),
                            Math.max(range.getFirstColumn(), other.getFirstColumn())));
            return false;
        }
        var covered = coveredValue(body, range);
        if (covered != null) {
            problem(line, "The cell %s has a value, but the merged range %s covers it".formatted(
                    covered.formatAsString(), range.formatAsString()),
                    header.cellOf(covered.getRow(), covered.getColumn()));
            return false;
        }
        return true;
    }

    /** Tells whether a range takes a cell another range already takes. */
    private static boolean overlapsTaken(CellRangeAddress range, Map<Integer, BitSet> taken) {
        for (var row = range.getFirstRow(); row <= range.getLastRow(); row++) {
            var columns = taken.get(row);
            var next = columns == null ? -1 : columns.nextSetBit(range.getFirstColumn());
            if (next >= 0 && next <= range.getLastColumn()) {
                return true;
            }
        }
        return false;
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
