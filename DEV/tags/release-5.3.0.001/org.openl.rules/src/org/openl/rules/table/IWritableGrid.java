/**
 * Created Feb 15, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.XlsBooleanFormat;
import org.openl.rules.table.xls.XlsDateFormat;
import org.openl.rules.table.xls.XlsNumberFormat;
import org.openl.rules.table.xls.XlsSheetGridExporter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import static org.openl.rules.table.xls.XlsSheetGridExporter.SHEET_NAME;
import org.openl.util.export.IExporter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * @author snshor
 * 
 */
public interface IWritableGrid extends IGrid {
    static public class Tool {
        static final boolean COLUMNS = true, ROWS = false, INSERT = true, REMOVE = false;        

        public static IExporter createExporter(IWritableGrid wGrid) {
            if (wGrid instanceof XlsSheetGridModel) {
                return new XlsSheetGridExporter((XlsSheetGridModel) wGrid);
            }

            return null;
        }

        public static IExporter createExporter(XlsWorkbookSourceCodeModule workbookModule) {
            Workbook workbook = workbookModule.getWorkbook();
            Sheet sheet;
            synchronized (workbook) {
                sheet = workbook.getSheet(SHEET_NAME);
                if (sheet == null) {
                    sheet = workbook.createSheet(SHEET_NAME);
                }
            }

            return new XlsSheetGridExporter(workbook, new XlsSheetGridModel(sheet));
        }

        public static CellMetaInfo getCellMetaInfo(IGrid grid, int col, int row) {
            IWritableGrid wgrid = getWritableGrid(grid);
            if (wgrid == null) {
                return null;
            }

            return wgrid.getCellMetaInfo(col, row);
        }

        public static CellMetaInfo getCellMetaInfo(IGridTable table, int col, int row) {
            IWritableGrid wgrid = getWritableGrid(table);
            if (wgrid == null) {
                return null;
            }
            int gcol = table.getGridColumn(col, row);
            int grow = table.getGridRow(col, row);

            return wgrid.getCellMetaInfo(gcol, grow);
        }

        public static ICellStyle getCellStyle(IGrid grid, int col, int row) {
            IWritableGrid wgrid = getWritableGrid(grid);
            if (wgrid == null) {
                return null;
            }

            return wgrid.getCell(col, row).getStyle();
        }

        public static IWritableGrid getWritableGrid(IGrid grid) {
            if (grid instanceof IWritableGrid) {
                return (IWritableGrid) grid;
            }
            return null;
        }

        public static IWritableGrid getWritableGrid(IGridTable table) {
            IGrid grid = table.getGrid();
            if (grid instanceof IWritableGrid) {
                return (IWritableGrid) grid;
            }
            return null;
        }

        /**
         * Searches all merged regions inside the specified region of table for
         * regions that have to be resized.
         * 
         * @param wgrid Current writable grid.
         * @param firstRowOrColumn Index of row or column for
         *            insertion/removing.
         * @param numberOfRowsOrColumns Number of elements to insert/remove.
         * @param isInsert Flag that defines what we have to do(insert/remove).
         * @param isColumns Flag that defines direction of insertion/removing.
         * @param regionOfTable Region of current table.
         * @return All actions to resize merged regions.
         */
        public static List<IUndoableGridAction> resizeMergedRegions(IWritableGrid wgrid, int firstRowOrColumn,
                int numberOfRowsOrColumns, boolean isInsert, boolean isColumns, IGridRegion regionOfTable) {
            ArrayList<IUndoableGridAction> resizeActions = new ArrayList<IUndoableGridAction>();
            for (int i = 0; i < wgrid.getNumberOfMergedRegions(); i++) {
                IGridRegion existingMergedRegion = wgrid.getMergedRegion(i);
                // merged region is contained by region of grid
                if (IGridRegion.Tool.contains(regionOfTable, existingMergedRegion.getLeft(),
                        existingMergedRegion.getTop())) {
                    if (isColumns) {
                        // merged region contains column which we copy
                        if (IGridRegion.Tool.width(existingMergedRegion) > 1 // merged by columns region
                                && IGridRegion.Tool.contains(existingMergedRegion, regionOfTable.getLeft()
                                + firstRowOrColumn, existingMergedRegion.getTop())) {
                            resizeActions.add(new UndoableResizeRegionAction(existingMergedRegion,
                                    numberOfRowsOrColumns, isInsert, isColumns));
                        }
                    } else {
                        // merged region contains row which we copy
                        if (IGridRegion.Tool.height(existingMergedRegion) > 1 // merged by rows region
                                && IGridRegion.Tool.contains(existingMergedRegion, existingMergedRegion
                                .getLeft(), regionOfTable.getTop() + firstRowOrColumn)) {
                            resizeActions.add(new UndoableResizeRegionAction(existingMergedRegion,
                                    numberOfRowsOrColumns, isInsert, isColumns));
                        }
                    }
                }
            }
            return resizeActions;
        }

        public static IUndoableGridAction insertColumns(int nColumns, int beforeColumns, IGridRegion region,
                IWritableGrid wgrid) {
            int h = IGridRegion.Tool.height(region);
            int w = IGridRegion.Tool.width(region);
            int columnsToMove = w - beforeColumns;
            int top = region.getTop();

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(h * columnsToMove);

            // move columns
            for (int i = 0; i < columnsToMove; i++) {
                int col = region.getRight() - i;
                for (int j = 0; j < h; j++) {
                    // wgrid.copyCell(col, top+j, col + nColumns, top+j);
                    actions.add(new UndoableCopyValueAction(col, top + j, col + nColumns, top + j));
                }
            }

            actions.addAll(resizeMergedRegions(wgrid, beforeColumns, nColumns, INSERT, COLUMNS, region));
            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridAction insertRows(int nRows, int beforeRow, IGridRegion region, IWritableGrid wgrid) {
            int h = IGridRegion.Tool.height(region);
            int w = IGridRegion.Tool.width(region);
            int rowsToMove = h - beforeRow;
            int left = region.getLeft();

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * rowsToMove);

            // move row
            for (int i = 0; i < rowsToMove; i++) {
                int row = region.getBottom() - i;
                for (int j = 0; j < w; j++) {
                    actions.add(new UndoableCopyValueAction(left + j, row, left + j, row + nRows));
                }
            }

            actions.addAll(resizeMergedRegions(wgrid, beforeRow, nRows, INSERT, ROWS, region));
            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridAction insertProp(IGridRegion region, IWritableGrid wgrid,
                String propName, String propValue) {
            IGridFilter filter = getFilter(propName);
            int h = IGridRegion.Tool.height(region);
            int w = IGridRegion.Tool.width(region);
            int nRows = 1;
            int beforeRow = 1;

            int left = region.getLeft();
            int top = region.getTop();

            boolean bProps = false;
            String propsHeader = wgrid.getCell(left, top + 1).getStringValue();
            if (propsHeader != null && propsHeader.equals("properties")) {
                bProps = true;
            }
            int propsCount = 0;
            if (bProps) {
                propsCount = wgrid.getCell(left, top + 1).getHeight();
                for (int i = 0; i < propsCount; i++) {
                    String pName = wgrid.getCell(left + 1, top + 1 + i).getStringValue();
                    if (pName.equals(propName)) {
                        return new UndoableSetValueAction(left + 2, top + 1 + i, propValue, filter);
                    }
                }
            }

            int rowsToMove = h - beforeRow;

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * rowsToMove);

            // move row
            for (int i = 0; i < rowsToMove; i++) {
                int row = region.getBottom() - i;
                for (int j = 0; j < w; j++) {
                    actions.add(new UndoableCopyValueAction(left + j, row, left + j, row + nRows));
                }
            }

            if (!bProps) {
                actions.add(new UndoableSetValueAction(left, top + beforeRow, "properties", null));
                if (w > 3) {
                    // clear cells
                    for (int j = left + 3; j < left + w; j++) {
                        actions.add(new UndoableClearAction(j, top + beforeRow));
                    }
                }
            }
            actions.add(new UndoableSetValueAction(left + 1, top + beforeRow, propName, null));
            actions.add(new UndoableSetValueAction(left + 2, top + beforeRow, propValue, filter));

            if (propsCount == 1) {
                // resize 'properties' cell
                actions.add(new UndoableResizeRegionAction(new GridRegion(top + 1, left,
                        top + 1, left), nRows, INSERT, ROWS));
            } else {
                actions.addAll(resizeMergedRegions(wgrid, beforeRow, nRows, INSERT, ROWS, region));
            }

            return new UndoableCompositeAction(actions);
        }
        
        private static IGridFilter getFilter(String propName) {
            IGridFilter result = null;
            TablePropertyDefinition tablProp= DefaultPropertyDefinitions.getPropertyByName(propName);
            if(tablProp != null) {
                if(String.class.equals(tablProp.getType().getInstanceClass())) {
                    result = null;
                } else {
                    if(Date.class.equals(tablProp.getType().getInstanceClass())) {
                        result = new XlsDateFormat(tablProp.getFormat()); 
                    } else {
                        if(Boolean.class.equals(tablProp.getType().getInstanceClass())) {
                            result = new XlsBooleanFormat();
                        } else {
                            if(Integer.class.equals(tablProp.getType().getInstanceClass())) {                                
                                result = XlsNumberFormat.General;
                            } else {
                                if(Double.class.equals(tablProp.getType().getInstanceClass())) {
                                    result = XlsNumberFormat.General;
                                }
                            }
                                
                        }
                    }
                }
            }
            return result;
        }

        public static void putCellMetaInfo(IGridTable table, int col, int row, CellMetaInfo meta) {
            IWritableGrid wgrid = getWritableGrid(table);
            if (wgrid == null) {
                return;
            }
            int gcol = table.getGridColumn(col, row);
            int grow = table.getGridRow(col, row);

            wgrid.setCellMetaInfo(gcol, grow, meta);
        }

        public static IUndoableGridAction removeColumns(int nCols, int startColumn, IGridRegion region,
                IWritableGrid wgrid) {
            int firstToMove = startColumn + nCols;
            int w = IGridRegion.Tool.width(region);
            int h = IGridRegion.Tool.height(region);
            int left = region.getLeft();
            int top = region.getTop();

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(h * (w - startColumn));
            for (int i = firstToMove; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    actions.add(new UndoableCopyValueAction(left + i, top + j, left + i - nCols, top + j));
                }
            }
            for (int i = 0; i < nCols; i++) {
                for (int j = 0; j < h; j++) {
                    actions.add(new UndoableClearAction(left + w - 1 - i, top + j));
                }
            }

            //actions.addAll(resizeMergedRegions(wgrid, startColumn, nCols, REMOVE, INSERT, region));
            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridAction removeRows(int nRows, int startRow, IGridRegion region, IWritableGrid wgrid) {
            int firstToMove = startRow + nRows;
            int w = IGridRegion.Tool.width(region);
            int h = IGridRegion.Tool.height(region);
            int left = region.getLeft();
            int top = region.getTop();

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * (h - startRow));
            for (int i = firstToMove; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    actions.add(new UndoableCopyValueAction(left + j, top + i, left + j, top + i - nRows));
                }
            }
            for (int i = 0; i < nRows; i++) {
                for (int j = 0; j < w; j++) {
                    actions.add(new UndoableClearAction(left + j, top + h - 1 - i));
                }
            }

            //actions.addAll(resizeMergedRegions(wgrid, startRow, nRows, REMOVE, ROWS, region));
            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridAction setStringValue(int col, int row, IGridRegion region, String value,
                IGridFilter filter) {

            int gcol = region.getLeft() + col;
            int grow = region.getTop() + row;

            // wgrid.setCellStringValue(gcol, grow, value);
            return new UndoableSetValueAction(gcol, grow, value, filter);
        }

        public static IUndoableGridAction setStringValue(int col, int row, IGridTable table, String value,
                IGridFilter filter) {
            // IWritableGrid wgrid = getWritableGrid(table);
            int gcol = table.getGridColumn(col, row);
            int grow = table.getGridRow(col, row);

            // wgrid.setCellStringValue(gcol, grow, value);
            return new UndoableSetValueAction(gcol, grow, value, filter);

        }

        public static IUndoableGridAction setStyle(int col, int row, IGridRegion region, ICellStyle style) {
            int gcol = region.getLeft() + col;
            int grow = region.getTop() + row;
            return new UndoableSetStyleAction(gcol, grow, style);
        }

        public static IUndoableGridAction setStyle(int col, int row, IGridTable table, ICellStyle style) {
            int gcol = table.getGridColumn(col, row);
            int grow = table.getGridRow(col, row);
            return new UndoableSetStyleAction(gcol, grow, style);
        }
    }
    
    public static final String DATE_FORMAT = "MM/dd/yyyy";

    /**
     * @param reg
     */
    int addMergedRegion(IGridRegion reg);

    void clearCell(int col, int row);

    void copyCell(int colFrom, int rowFrom, int colTo, int RowTo);

    /**
     * Finds a rectangular area of given width and height on the grid that can
     * be used for writing. The returned region should not intersect with or
     * touch existing not empty cells.
     * 
     * @param width rectangle width
     * @param height rectangle height
     * @return region representing required rectangle or <code>null</code> if
     *         not found
     */
    IGridRegion findEmptyRect(int width, int height);

    CellMetaInfo getCellMetaInfo(int col, int row);

    /**
     * @param to
     */
    void removeMergedRegion(IGridRegion to);

    void setCellMetaInfo(int col, int row, CellMetaInfo meta);

    void setCellStringValue(int col, int row, String value);

    void setCellStyle(int col, int row, ICellStyle style);

    void setCellValue(int col, int row, Object value);

}
