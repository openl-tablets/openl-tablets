package org.openl.excel.grid;

import java.util.Arrays;
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
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInfo;

/**
 * The errors of a Markdown file that breaks the format.
 *
 * <p>An error points at what shows the problem: the cell of the table, or the line of the file when no cell of the
 * table shows it, as for a file that holds no table at all.
 *
 * <p>The message tells what is wrong and nothing else, as the message of an error in a workbook does: the place of
 * the problem comes with the address of the error.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownFormatErrors {

    /**
     * Returns the error of a problem of a file.
     *
     * @param file   the file
     * @param sheets the sheets read from the file, holding the table the problem may show in
     */
    static SyntaxNodeException of(MarkdownProblem problem, XlsWorkbookSourceCodeModule file,
                                  WorksheetSyntaxNode... sheets) {
        var message = problem.problem();
        return Optional.ofNullable(problem.cell())
                .flatMap(cell -> cellOf(sheets, cell))
                .map(cell -> SyntaxNodeExceptionUtils.createError(message, null, null, cell))
                .orElseGet(() -> SyntaxNodeExceptionUtils.createError(message, null, lineOf(file, problem.line()),
                        file));
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
     * Returns the text of a line of the file, from its first character to its last, the indent left out: a property
     * is indented under {@code properties}.
     */
    private static ILocation lineOf(XlsWorkbookSourceCodeModule file, int line) {
        var text = new TextInfo(file.getCode());
        var written = text.getLine(line - 1);
        var start = text.getPosition(line - 1) + written.length() - written.stripLeading().length();
        var length = written.strip().length();
        return LocationUtils.createTextInterval(start, start + Math.max(length - 1, 0));
    }
}
