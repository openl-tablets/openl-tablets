package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;

/**
 * Reads any table in raw format as a 2D matrix with explicit merge information.
 * <p>
 * This reader works with any table type (Data, Test, Spreadsheet, etc.) without interpreting or parsing
 * the table structure. It preserves the exact layout including merged cells through colspan/rowspan values.
 * <p>
 * Implementation uses {@code TableModel} from TableEditor component to properly handle:
 * <ul>
 *   <li>Row and column spans (merged cells with colspan/rowspan)
 *   <li>Cell content extraction with formula evaluation
 *   <li>Proper table dimensions (height/width)
 *   <li>All table types (works consistently across different table implementations)
 * </ul>
 * <p>
 * The resulting {@code RawTableView} contains:
 * <ul>
 *   <li>A 2D matrix ({@code source}) of {@code RawTableCell} objects
 *   <li>Each cell with explicit colspan/rowspan for merged regions
 *   <li>Covered cells marked with {@code covered=true} to indicate they're masked by another cell's span
 *   <li>No type information or validation - purely structural
 * </ul>
 *
 * @author Vladyslav Pikus
 */
@Component
public class RawTableReader extends TableReader<RawTableView, RawTableView.Builder> {

    public RawTableReader() {
        super(RawTableView::builder);
    }

    /**
     * Initialize RawTableView by reading the table as a raw 2D matrix with merge information.
     * <p>
     * Creates a TableModel from the IGridTable to properly handle:
     * <ul>
     *   <li>Cell merging with colspan/rowspan extraction
     *   <li>Cell content retrieval (formulas are evaluated to values)
     *   <li>Correct table dimensions
     * </ul>
     *
     * @param builder    The RawTableView builder to populate
     * @param openLTable The table to read in raw format
     */
    @Override
    protected void initialize(RawTableView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);
        var metaInfoReader = openLTable.getSyntaxNode().getMetaInfoReader();
        // Use TableModel to properly handle cell merging and content
        var tableModel = TableModel.initializeTableModel(openLTable.getGridTable(), -1, metaInfoReader);

        if (tableModel != null) {
            // Convert TableModel to raw source
            var cellValueReader = new CellValueReader(metaInfoReader);
            List<List<RawTableCell>> source = convertTableModelToMatrix(tableModel, cellValueReader);
            builder.source(source);
        }
    }

    /**
     * Convert TableModel to raw 2D matrix representation with explicit span information.
     * <p>
     * Processing:
     * <ul>
     *   <li>Iterates through all cells in the TableModel
     *   <li>Extracts colspan/rowspan for each cell from CellModel
     *   <li>Retrieves cell values using the provided cellValueReader function
     *   <li>Marks cells that are covered by other cells' spans with RawTableCell.COVERED
     *   <li>Creates RawTableCell objects with explicit colspan/rowspan for origin cells
     * </ul>
     * <p>
     * Covered cells (those within a merged region but not the origin cell) are marked with
     * {@code RawTableCell.COVERED} to indicate they should be skipped during processing.
     *
     * @param tableModel      The TableModel containing cell layout and span information
     * @param cellValueReader Function to extract cell values from ICell instances
     * @return 2D list of RawTableCell objects representing the table matrix
     */
    private List<List<RawTableCell>> convertTableModelToMatrix(TableModel tableModel, Function<ICell, Object> cellValueReader) {
        List<List<RawTableCell>> matrix = new ArrayList<>();

        var cells = tableModel.getCells();
        int height = tableModel.getHeight();
        int width = height > 0 ? cells[0].length : 0;

        // Track which cells have already been covered as merged cells
        Set<CellRef> coveredCells = new HashSet<>();

        for (int row = 0; row < height; row++) {
            List<RawTableCell> rowCells = new ArrayList<>();

            for (int col = 0; col < width; col++) {
                if (coveredCells.contains(new CellRef(row, col))) {
                    // This cell was already covered as part of a merged region (covered by another cell's span)
                    rowCells.add(RawTableCell.COVERED);
                    continue;
                }

                var cm = (CellModel) cells[row][col];
                RawTableCell rawCell;
                // Extract cell value
                var cell = tableModel.getGridTable().getCell(cm.getColumn(), cm.getRow());
                Object value = cellValueReader.apply(cell);

                // Check for merging
                int rowspan = cm.getRowspan();
                int colspan = cm.getColspan();

                if (colspan > 1 || rowspan > 1) {
                    // This is a merged cell origin with explicit colspan/rowspan
                    rawCell = RawTableCell.withSpan(value, colspan, rowspan);

                    // Mark spanned cells as covered
                    for (int r = row; r < row + rowspan && r < height; r++) {
                        for (int c = col; c < col + colspan && c < width; c++) {
                            if (r > row || c > col) {
                                coveredCells.add(new CellRef(r, c));
                            }
                        }
                    }
                } else {
                    // Simple cell without merging
                    rawCell = RawTableCell.simple(value);
                }
                rowCells.add(rawCell);
            }

            matrix.add(rowCells);
        }

        return matrix;
    }

    private record CellRef(int row, int col) {
    }

}
