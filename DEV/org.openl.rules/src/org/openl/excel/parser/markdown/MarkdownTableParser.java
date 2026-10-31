package org.openl.excel.parser.markdown;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.poi.ss.util.CellAddress;
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

import org.openl.util.StringUtils;

/**
 * Reads the rules table of a Markdown file. A file holds one table at most.
 *
 * <p>The header of the table is the front matter of the file: the {@code table} key holds the text of the first table
 * cell, such as {@code Rules String Greeting(Integer hour)}. The {@code properties} key holds the table properties, a
 * name and a value on each line under it. Values are read as written: {@code NO} stays {@code NO}, and a list is joined
 * with commas. Any other key is a problem: the table is read without it.
 *
 * <p>The text between the front matter and the body is the description of the table, its {@code description}
 * property; a quote and an HTML comment there are comments, and no part of it (see {@link MarkdownDescription}). A
 * {@code description} under {@code properties} is a problem: the table is read without it.
 *
 * <p>The body is the first GFM table, HTML table or code block after the front matter. Every row of a table, its first
 * row included, is a row of the body; a code block is the only body cell, as the code of a {@code Method}. A body with
 * merged cells is an HTML table, whose cells span them with {@code rowspan} and {@code colspan} (see
 * {@link HtmlTableReader}); a GFM table has none (see {@link GfmTableReader}). A file with no body holds a table of a
 * header alone, such as a {@code Datatype} that adds no field to its parent; the text after its front matter is its
 * description.
 *
 * <p>A cell is taken as written, not rendered as Markdown. A cell wrapped in backticks as a whole is unwrapped,
 * {@code \|} gives a vertical bar and {@code <br>} a line break. A whole number, {@code true} and {@code false}, and a
 * date, in a cell or a property, read as the values a cell of a workbook holds, and any other text as a text, see
 * {@link MarkdownValues}.
 *
 * <p>The text after the body is documentation. A file without front matter is documentation as a whole.
 *
 * <p>A file that breaks the format in the front matter holds no table. One that breaks it in the body keeps the table
 * without the part that breaks it: the cells of a row beyond those of the first row, the part of a merged cell past the
 * table or over another, the rows a blank line cuts off. The file tells each such problem.
 */
final class MarkdownTableParser {

    private static final Parser PARSER = Parser.builder()
            // A table is known by its lines: its cells are cut out of the lines of the file, whatever their number
            .customBlockParserFactory(new TableLines.Factory())
            .customBlockParserFactory(new HtmlTableLines.Factory())
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            // A cell is taken from the text of the file, so the Markdown inside it is never parsed
            .inlineParserFactory(context -> (content, node) -> {
            })
            .build();
    private static final Pattern LINE_SEPARATOR = Pattern.compile("\r\n|\r|\n");
    private static final String ROW_OPEN = "<tr";
    private static final String ROW_CLOSE = "</tr>";
    private static final String FRONT_MATTER = "---";
    private static final String TABLE = "table";
    private static final String PROPERTIES = "properties";
    private static final String MERGES = "merges";

    private final String text;
    private final List<String> lines;
    private final List<MarkdownProblem> problems = new ArrayList<>();
    /** The rows of the table of the body, once read. */
    private List<TableRow> rows = List.of();
    /** Whether the table of the body is read as written, without a problem. */
    private boolean readAsWritten = true;

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
        var written = readHeader(String.join("\n", lines.subList(1, end)));

        // The front matter is blanked out rather than cut off, so the lines keep their numbers.
        var body = new ArrayList<>(lines);
        Collections.fill(body.subList(0, end + 1), "");
        var document = PARSER.parse(String.join("\n", body));
        var node = document.getFirstChild();
        var description = new ArrayList<TableFile.Block>();
        while (node != null && !isBody(node)) {
            if (!isComment(node)) {
                description.add(blockOf(node));
            }
            node = node.getNext();
        }
        var header = described(written, MarkdownDescription.of(lines, description));
        // A table with no table nor code block after its front matter has a header alone
        var table = readTable(header, node);
        if (node != null) {
            checkNotSplit(node);
        }
        var separator = LINE_SEPARATOR.matcher(text);
        var afterDescription = description.isEmpty() ? end + 1 : description.getLast().lastLine() + 1;
        return Optional.of(new TableFile(text,
                lines,
                separator.find() ? separator.group() : "\n",
                table,
                end,
                header.keys(),
                header.propertyKeys(),
                List.copyOf(description),
                node == null ? new TableFile.NoBody(afterDescription) : bodyOf(node),
                List.copyOf(problems)));
    }

    /**
     * Tells the body of a table: a GFM table, an HTML table, or a code block.
     */
    private static boolean isBody(Node node) {
        return node instanceof TableLines || node instanceof HtmlTableLines || node instanceof FencedCodeBlock;
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
        return new Header(header.text(), properties, header.keys(), header.propertyKeys());
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
                // The table is read without it, and the key stays in the file as written
                case MERGES -> problem(keyLine, """
                        Merged cells are written in an HTML table, with 'rowspan' and 'colspan', not under \
                        'merges'""", null);
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
        return new Header(header, properties, keys, propertyKeys);
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

    // Body

    private RulesTable readTable(Header header, @Nullable Node body) {
        return switch (body) {
            case null -> new RulesTable(header.text(), header.properties(), List.of(), List.of());
            case TableLines table -> laidOut(header, new GfmTableReader(lines, table.rowLines()));
            case HtmlTableLines table -> laidOut(header,
                    new HtmlTableReader(lines, table.firstLine(), table.lastLine()));
            default -> new RulesTable(header.text(), header.properties(),
                    List.of(Collections.singletonList(StringUtils.trimToNull(((FencedCodeBlock) body).getLiteral()))),
                    List.of());
        };
    }

    /**
     * Lays the rows of a table out in its body, whatever syntax writes the table. A part of the table that breaks the
     * format is a problem, and the table is read without it.
     */
    private RulesTable laidOut(Header header, MarkdownTableReader reader) {
        rows = reader.getRows();
        var laidOut = TableLayout.bodyOf(rows);
        readAsWritten = laidOut.problems().isEmpty();
        laidOut.problems()
                .forEach(problem -> problem(problem.line() + 1,
                        problem.problem(),
                        header.cellOf(problem.cell().getRow(), problem.cell().getColumn())));
        return new RulesTable(header.text(), header.properties(), laidOut.body(), laidOut.merges());
    }

    private TableFile.Body bodyOf(Node body) {
        return switch (body) {
            case FencedCodeBlock code -> {
                var spans = body.getSourceSpans();
                yield new TableFile.CodeBody(spans.getFirst().getLineIndex(),
                        spans.getLast().getLineIndex(),
                        code.getFenceCharacter().repeat(code.getOpeningFenceLength()),
                        StringUtils.trimToEmpty(code.getInfo()),
                        code.getFenceIndent());
            }
            case HtmlTableLines table -> new TableFile.HtmlBody(table.firstLine(),
                    table.lastLine(),
                    readAsWritten && table.closed() ? rowLinesOf(table) : List.of());
            default -> new TableFile.GfmBody(((TableLines) body).rowLines(), ((TableLines) body).delimiterLine());
        };
    }

    /**
     * Returns the line of each row of an HTML table, when the rows stand one to a line between the lines that open and
     * close the table, each line holding its row alone; none otherwise.
     */
    private List<Integer> rowLinesOf(HtmlTableLines table) {
        if (rows.isEmpty()) {
            return List.of();
        }
        var first = rows.getFirst().lines().firstLine();
        for (var i = 0; i < rows.size(); i++) {
            if (!standsAlone(rows.get(i), first + i)) {
                return List.of();
            }
        }
        var end = first + rows.size();
        return first > table.firstLine() && end <= table.lastLine()
                ? IntStream.range(first, end).boxed().toList()
                : List.of();
    }

    private boolean standsAlone(TableRow row, int line) {
        return row.lines().firstLine() == line && row.lines().lastLine() == line && isRowLine(lines.get(line).strip());
    }

    /** Tells a line of an HTML table that holds a row alone, as the writer writes it: {@code <tr>…</tr>}. */
    private static boolean isRowLine(String line) {
        var length = line.length();
        var opened = line.regionMatches(true, 0, ROW_OPEN, 0, ROW_OPEN.length()) && length > ROW_OPEN.length()
                && (line.charAt(ROW_OPEN.length()) == '>' || Character.isWhitespace(line.charAt(ROW_OPEN.length())));
        return opened && line.regionMatches(true, length - ROW_CLOSE.length(), ROW_CLOSE, 0, ROW_CLOSE.length());
    }

    /**
     * Tells of table rows cut off from the body by a blank line: GFM reads them as a paragraph, so they are lost. An
     * HTML table a blank line cuts is not closed.
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
        if (body instanceof HtmlTableLines table && !table.closed()) {
            problem(table.lastLine() + 1, """
                    The table is not closed with '</table>': a blank line or the end of the file cuts it off""",
                    null);
        }
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
     * @param keys         the keys of the front matter
     * @param propertyKeys the keys under {@code properties}
     */
    private record Header(String text,
                          SequencedMap<String, String> properties,
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
