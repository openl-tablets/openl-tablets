/**
 * Created Feb 15, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.XlsSheetGridExporter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import static org.openl.rules.table.xls.XlsSheetGridExporter.SHEET_NAME;
import org.openl.util.export.IExporter;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * @author snshor
 * 
 */
public interface IWritableGrid extends IGrid
{
	void copyCell(int colFrom, int rowFrom, int colTo, int RowTo);

	void clearCell(int col, int row);

	void setCellStringValue(int col, int row, String value);

	void setCellValue(int col, int row, Object value);

	void setCellType(int col, int row, int type);

	void setCellMetaInfo(int col, int row, CellMetaInfo meta);

	void setCellStyle(int col, int row, ICellStyle style);

	CellMetaInfo getCellMetaInfo(int col, int row);

    /**
     * Finds a rectangular area of given width and height on the grid that can be used for writing.
     * The returned region should not intersect with or touch existing not empty cells.   
     *
     * @param width rectangle width
     * @param height rectangle height
     * @return region representing required rectangle or <code>null</code> if not found
     */
    IGridRegion findEmptyRect(int width, int height);

    static public class Tool
	{
		public static IWritableGrid getWritableGrid(IGridTable table)
		{
			IGrid grid = table.getGrid();
			if (grid instanceof IWritableGrid)
				return (IWritableGrid) grid;
			return null;
		}

		public static IWritableGrid getWritableGrid(IGrid grid)
		{
			if (grid instanceof IWritableGrid)
				return (IWritableGrid) grid;
			return null;
		}

		public static void putCellMetaInfo(IGridTable table, int col, int row,
				CellMetaInfo meta)
		{
			IWritableGrid wgrid = getWritableGrid(table);
			if (wgrid == null)
				return;
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);

			wgrid.setCellMetaInfo(gcol, grow, meta);
		}

		public static CellMetaInfo getCellMetaInfo(IGridTable table, int col,
				int row)
		{
			IWritableGrid wgrid = getWritableGrid(table);
			if (wgrid == null)
				return null;
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);

			return wgrid.getCellMetaInfo(gcol, grow);
		}

		public static CellMetaInfo getCellMetaInfo(IGrid grid, int col, int row)
		{
			IWritableGrid wgrid = getWritableGrid(grid);
			if (wgrid == null)
				return null;

			return wgrid.getCellMetaInfo(col, row);
		}

		public static IUndoableGridAction removeColumns(int nCols, int startColumn,
				IGridRegion region)
		{
			int firstToMove = startColumn + nCols;
			int w = IGridRegion.Tool.width(region);
			int h = IGridRegion.Tool.height(region);
			int left = region.getLeft();
			int top = region.getTop();

			ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(h * (w - startColumn));
			for (int i = firstToMove; i < w; i++)
			{
				for (int j = 0; j < h; j++)
				{
					actions.add(new UndoableCopyValueAction(left + i, top + j, left + i
							- nCols, top + j));
				}
			}
			for (int i = 0; i < nCols; i++)
			{
				for (int j = 0; j < h; j++)
				{
					actions.add(new UndoableClearAction(left + w - 1 - i, top + j));
				}
			}
			return new UndoableCompositeAction(actions);
		}

		public static IUndoableGridAction removeRows(int nRows, int startRow,
				IGridRegion region)
		{
			int firstToMove = startRow + nRows;
			int w = IGridRegion.Tool.width(region);
			int h = IGridRegion.Tool.height(region);
			int left = region.getLeft();
			int top = region.getTop();

			ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * (h - startRow));
			for (int i = firstToMove; i < h; i++)
			{
				for (int j = 0; j < w; j++)
				{
					actions.add(new UndoableCopyValueAction(left + j, top + i, left + j,
							top + i - nRows));
				}
			}
			for (int i = 0; i < nRows; i++)
			{
				for (int j = 0; j < w; j++)
				{
					actions.add(new UndoableClearAction(left + j, top + h - 1 - i));
				}
			}
			return new UndoableCompositeAction(actions);
		}

		public static IUndoableGridAction insertRows(int nRows, int beforeRow,
				IGridRegion region, @SuppressWarnings("unused")
				IWritableGrid wgrid)
		{
			int h = IGridRegion.Tool.height(region);
			int w = IGridRegion.Tool.width(region);
			int rowsToMove = h - beforeRow;
			int left = region.getLeft();

			ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * rowsToMove);

			// move row
			for (int i = 0; i < rowsToMove; i++)
			{
				int row = region.getBottom() - i;
				for (int j = 0; j < w; j++)
				{
					// wgrid.copyCell(left+j, row, left+j, row + nRows);
					actions.add(new UndoableCopyValueAction(left + j, row, left + j, row
							+ nRows));
				}
			}
			return new UndoableCompositeAction(actions);
		}

		public static IUndoableGridAction setStringValue(int col, int row,
				IGridTable table, String value, IGridFilter filter)
		{
			// IWritableGrid wgrid = getWritableGrid(table);
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);

			// wgrid.setCellStringValue(gcol, grow, value);
			return new UndoableSetValueAction(gcol, grow, value, filter);

		}

		public static IUndoableGridAction setStringValue(int col, int row,
				IGridRegion region, String value, IGridFilter filter)
		{
			
			
			int gcol = region.getLeft() + col;
			int grow = region.getTop() + row;

			// wgrid.setCellStringValue(gcol, grow, value);
			return new UndoableSetValueAction(gcol, grow, value, filter);
		}

		public static IUndoableGridAction setStyle(int col, int row,
				IGridTable table, ICellStyle style)
		{
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);
			return new UndoableSetStyleAction(gcol, grow, style);
		}

		public static IUndoableGridAction setStyle(int col, int row,
				IGridRegion region, ICellStyle style)
		{
			int gcol = region.getLeft() + col;
			int grow = region.getTop() + row;
			return new UndoableSetStyleAction(gcol, grow, style);
		}

		public static ICellStyle getCellStyle(IGrid grid, int col, int row)
		{
			IWritableGrid wgrid = getWritableGrid(grid);
			if (wgrid == null)
				return null;

			return wgrid.getCellStyle(col, row);
		}

		public static IUndoableGridAction insertColumns(int nColumns,
				int beforeColumns, IGridRegion region, @SuppressWarnings("unused")
				IWritableGrid wgrid)
		{
			int h = IGridRegion.Tool.height(region);
			int w = IGridRegion.Tool.width(region);
			int columnsToMove = w - beforeColumns;
			int top = region.getTop();

			ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(
					h * columnsToMove);

			// move columns
			for (int i = 0; i < columnsToMove; i++)
			{
				int col = region.getRight() - i;
				for (int j = 0; j < h; j++)
				{
					// wgrid.copyCell(col, top+j, col + nColumns, top+j);
					actions.add(new UndoableCopyValueAction(col, top + j, col + nColumns,
							top + j));
				}
			}

			return new UndoableCompositeAction(actions);
		}

        public static IExporter createExporter(IWritableGrid wGrid) {
            if (wGrid instanceof XlsSheetGridModel) {
                return new XlsSheetGridExporter((XlsSheetGridModel) wGrid);
            }

            return null;
        }

        public static IExporter createExporter(XlsWorkbookSourceCodeModule workbookModule) {
            HSSFWorkbook workbook = workbookModule.getWorkbook();
            HSSFSheet sheet;
            synchronized (workbook) {
                sheet = workbook.getSheet(SHEET_NAME);
                if (sheet == null) {
                    sheet = workbook.createSheet(SHEET_NAME);
                }
            }

            return new XlsSheetGridExporter(workbook, new XlsSheetGridModel(sheet));
        }
    }

	/**
	 * @param to
	 */
	void removeMergedRegion(IGridRegion to);

	/**
	 * @param reg
	 */
	int addMergedRegion(IGridRegion reg);

}
