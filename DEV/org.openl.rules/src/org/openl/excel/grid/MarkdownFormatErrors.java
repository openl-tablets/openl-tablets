package org.openl.excel.grid;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.util.CellAddress;

import org.openl.excel.parser.markdown.MarkdownProblem;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.util.TextRange;
import org.openl.util.text.ILocation;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInfo;

/**
 * The errors of a Markdown file that breaks the format.
 *
 * <p>An error points at what shows the problem: the cell of the table, the header of the table for a problem no cell
 * shows, such as a key of the front matter, or the line of the file when the file holds no table at all. It tells the
 * place of the problem in the file as well ({@link MarkdownFormatError}).
 *
 * <p>The message tells what is wrong and nothing else, as the message of an error in a workbook does: the place of
 * the problem comes with the address of the error.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownFormatErrors {

    private static final String BYTE_ORDER_MARK = "\uFEFF";

    /**
     * Returns the errors of the problems of a file. The text of the file is read once for all of them.
     *
     * @param file   the file
     * @param sheets the sheets read from the file, holding the table a problem may show in
     */
    static List<SyntaxNodeException> of(List<MarkdownProblem> problems, XlsWorkbookSourceCodeModule file,
                                        WorksheetSyntaxNode... sheets) {
        if (problems.isEmpty()) {
            return List.of();
        }
        var text = new TextInfo(file.getCode());
        return problems.stream().map(problem -> of(problem, text, file, sheets)).toList();
    }

    private static SyntaxNodeException of(MarkdownProblem problem, TextInfo text, XlsWorkbookSourceCodeModule file,
                                          WorksheetSyntaxNode[] sheets) {
        var message = problem.problem();
        var shown = Shown.of(text, problem.line());
        return Optional.ofNullable(problem.cell())
                .flatMap(cell -> cellOf(sheets, cell))
                .or(() -> headerOf(sheets))
                .map(cell -> new MarkdownFormatError(message, null, cell, shown.place()))
                .orElseGet(() -> new MarkdownFormatError(message, shown.location(), file, shown.place()));
    }

    /** The header of the table of the file, which the front matter writes: none for a file without a table. */
    private static Optional<GridCellSourceCodeModule> headerOf(WorksheetSyntaxNode[] sheets) {
        return Arrays.stream(sheets)
                .flatMap(sheet -> Arrays.stream(sheet.getTableSyntaxNodes()))
                .findFirst()
                .map(table -> new GridCellSourceCodeModule(table.getGridTable(), 0, 0, null));
    }

    private static Optional<GridCellSourceCodeModule> cellOf(WorksheetSyntaxNode[] sheets, CellAddress cell) {
        return Arrays.stream(sheets)
                .flatMap(sheet -> Arrays.stream(sheet.getTableSyntaxNodes()))
                .map(TableSyntaxNode::getGridTable)
                .filter(table -> GridRegionUtils.contains(table.getRegion(), cell.getColumn(), cell.getRow()))
                .findFirst()
                .map(table -> cellOf(table, cell));
    }

    private static GridCellSourceCodeModule cellOf(IGridTable table, CellAddress cell) {
        var region = table.getRegion();
        return new GridCellSourceCodeModule(table,
                cell.getColumn() - region.getLeft(),
                cell.getRow() - region.getTop(),
                null);
    }

    /**
     * The text of a line of the file as an editor of the file shows it: without the byte order mark of UTF-8 before
     * the first line, and its indent apart from the rest, as a property is indented under {@code properties}.
     *
     * @param line   the line, counted from 1
     * @param offset where the line starts in the text of the file, after the mark
     * @param indent the characters of the indent
     * @param length the characters of the rest, the spaces at its end left out
     */
    private record Shown(int line, int offset, int indent, int length) {

        static Shown of(TextInfo text, int line) {
            var written = text.getLine(line - 1);
            var mark = line == 1 && written.startsWith(BYTE_ORDER_MARK) ? BYTE_ORDER_MARK.length() : 0;
            var shown = written.substring(mark);
            return new Shown(line,
                    text.getPosition(line - 1) + mark,
                    shown.length() - shown.stripLeading().length(),
                    shown.strip().length());
        }

        /** The text of the line in the text of the file, from its first character to its last. */
        ILocation location() {
            var start = offset + indent;
            return LocationUtils.createTextInterval(start, start + Math.max(length - 1, 0));
        }

        /** Where the text of the line is, as an editor of the file counts its columns. */
        TextRange place() {
            return new TextRange(line, indent, line, indent + length);
        }
    }
}
