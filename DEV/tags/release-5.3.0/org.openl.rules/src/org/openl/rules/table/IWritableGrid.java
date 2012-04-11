/**
 * Created Feb 15, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.actions.IUndoableGridAction;
import org.openl.rules.table.actions.UndoableClearAction;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableCopyValueAction;
import org.openl.rules.table.actions.UndoableResizeMergedRegionAction;
import org.openl.rules.table.actions.UndoableSetStyleAction;
import org.openl.rules.table.actions.UndoableSetValueAction;
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
import org.apache.commons.lang.StringUtils;
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
                if (IGridRegion.Tool.contains(regionOfTable, existingMergedRegion.getLeft(), existingMergedRegion
                        .getTop())) {
                    if (isRegionMustBeResized(existingMergedRegion, firstRowOrColumn, numberOfRowsOrColumns, 
                            isColumns, regionOfTable)) {
                        resizeActions.add(new UndoableResizeMergedRegionAction(existingMergedRegion, numberOfRowsOrColumns,
                                isInsert, isColumns));
                    }
                }
            }
            return resizeActions;
        }

        /**
         * Checks if the specified region must be resized.
         *
         * If we delete all we remove all rows/columns in region then region must be deleted(not resized).
         */
        private static boolean isRegionMustBeResized(IGridRegion region, int firstRowOrColumn,
                int numberOfRowsOrColumns, boolean isColumns, IGridRegion regionOfTable) {
            if (isColumns) {
                // merged region contains column which we copy/remove
                if (IGridRegion.Tool.width(region) > numberOfRowsOrColumns
                        && IGridRegion.Tool.contains(region,regionOfTable.getLeft() +firstRowOrColumn, region.getTop())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // merged region contains row which we copy/remove
                if (IGridRegion.Tool.height(region) > numberOfRowsOrColumns
                        && IGridRegion.Tool.contains(region, region.getLeft(), regionOfTable.getTop() +  firstRowOrColumn)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        
        public static IUndoableGridAction insertColumns(int nColumns, int beforeColumns, IGridRegion region,
                IWritableGrid wgrid) {
            int h = IGridRegion.Tool.height(region);
            int w = IGridRegion.Tool.width(region);
            int columnsToMove = w - beforeColumns;

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(h * columnsToMove);

            int firstToMove = region.getLeft() + beforeColumns;
            actions.addAll(shiftColumns(firstToMove, nColumns, INSERT, region));
            actions.addAll(resizeMergedRegions(wgrid, beforeColumns, nColumns, INSERT, COLUMNS, region));

            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridAction insertRows(int nRows, int beforeRow, IGridRegion region, IWritableGrid wgrid) {
            int h = IGridRegion.Tool.height(region);
            int w = IGridRegion.Tool.width(region);
            int rowsToMove = h - beforeRow;

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * rowsToMove);

            int firstToMove = region.getTop() + beforeRow;
            actions.addAll(shiftRows(firstToMove, nRows, INSERT, region));
            actions.addAll(resizeMergedRegions(wgrid, beforeRow, nRows, INSERT, ROWS, region));

            return new UndoableCompositeAction(actions);
        }

        /**
         * Checks if the table specified by its region contains property. 
         */
        public static CellKey getPropertyCoordinates(IGridRegion region, IWritableGrid wgrid,
                String propName){
            int left = region.getLeft();
            int top = region.getTop();

            String propsHeader = wgrid.getCell(left, top + 1).getStringValue();
            if (propsHeader == null || !propsHeader.equals("properties")) {
                //there is no properties
                return null;
            }
            int propsCount = wgrid.getCell(left, top + 1).getHeight();
            for (int i = 0; i < propsCount; i++) {
                String pName = wgrid.getCell(left + 1, top + 1 + i).getStringValue();
                if (pName.equals(propName)) {
                    return new CellKey(1, 1 + i);
                }
            }
            return null;
        }

        /**
         * TODO To refactor
         * 
         * @return null if set new property with empty or same value
         * */
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
                        String pValue = wgrid.getCell(left + 2, top + 1 + i).getStringValue();
                        if (pValue!= null && propValue!= null &&  pValue.trim().equals(propValue.trim())) {
                            return null;
                        }
                        return new UndoableSetValueAction(left + 2, top + 1 + i, propValue, filter);
                    }
                }
            }

            if (StringUtils.isBlank(propValue)) {
                return null;
            }

            int rowsToMove = h - beforeRow;

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * rowsToMove);

            int firstToMove = region.getTop() + beforeRow;
            actions.addAll(shiftRows(firstToMove, nRows, INSERT, region));

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
                actions.add(new UndoableResizeMergedRegionAction(new GridRegion(top + 1, left,
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
                } else if(Date.class.equals(tablProp.getType().getInstanceClass())) {
                    result = new XlsDateFormat(tablProp.getFormat()); 
                } else if(Boolean.class.equals(tablProp.getType().getInstanceClass())) {
                    result = new XlsBooleanFormat();
                } else if(Integer.class.equals(tablProp.getType().getInstanceClass())) {                                
                    result = XlsNumberFormat.General;
                } else if(Double.class.equals(tablProp.getType().getInstanceClass())) {
                    result = XlsNumberFormat.General;
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
        
        private static List<IUndoableGridAction> clearCells(int startColumn,int nCols, int startRow,int nRows){
            ArrayList<IUndoableGridAction> clearActions = new ArrayList<IUndoableGridAction>();
            for (int i = startColumn; i < startColumn + nCols; i++) {
                for (int j = startRow; j < startRow + nRows; j++) {
                    clearActions.add(new UndoableClearAction(i, j));
                }
            }
            return clearActions;
        }

        /**
         * Checks if cell is the top left cell in merged region.
         * We don't have to remove value from this cell because value of merged
         * cell will be lost.
         * 
         */
        @SuppressWarnings("unused")
        private static boolean isTopLeftInMergedRegion(int column, int row, IWritableGrid wgrid) {
            for (int i = 0; i < wgrid.getNumberOfMergedRegions(); i++) {
                IGridRegion existingMergedRegion = wgrid.getMergedRegion(i);
                if (existingMergedRegion.getLeft() == column
                        && existingMergedRegion.getTop() == row) {
                    return true;
                }
            }
            return false;
        }

        private static List<IUndoableGridAction> shiftColumns(int startColumn, int nCols,boolean isInsert, IGridRegion region){
            ArrayList<IUndoableGridAction> shiftActions = new ArrayList<IUndoableGridAction>();
            int direction, colFromCopy, colToCopy;
            if (isInsert) {// shift columns left
                direction = -1;
                colFromCopy = region.getRight();
            } else {// shift columns right
                direction = 1;
                colFromCopy = startColumn;
            }
            for (int i = 0; i <= region.getRight() - startColumn; i++) {
                colToCopy = colFromCopy - direction * nCols;
                for (int row = region.getTop(); row <= region.getBottom(); row++) {
                    shiftActions.add(new UndoableCopyValueAction(colFromCopy, row, colToCopy, row));
                }
                colFromCopy += direction;
            }
            return shiftActions;
        }

        private static List<IUndoableGridAction> shiftRows(int startRow, int nRows, boolean isInsert, IGridRegion region) {
            ArrayList<IUndoableGridAction> shiftActions = new ArrayList<IUndoableGridAction>();
            int direction, rowFromCopy, rowToCopy;
            if (isInsert) {// shift rows up
                direction = -1;
                rowFromCopy = region.getBottom();
            } else {// shift rows down
                direction = 1;
                rowFromCopy = startRow;
            }
            for (int i = 0; i <= region.getBottom() - startRow; i++) {
                rowToCopy = rowFromCopy - direction * nRows;
                for (int column = region.getLeft(); column <= region.getRight(); column++) {
                    shiftActions.add(new UndoableCopyValueAction(column, rowFromCopy, column, rowToCopy));
                }
                rowFromCopy += direction;
            }
            return shiftActions;
        }

        public static IUndoableGridAction removeColumns(int nCols, int startColumn, IGridRegion region,
                IWritableGrid wgrid) {
            int firstToMove = region.getLeft() + startColumn + nCols;
            int w = IGridRegion.Tool.width(region);
            int h = IGridRegion.Tool.height(region);

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(h * (w - startColumn));

            actions.addAll(shiftColumns(firstToMove, nCols, REMOVE, region));
            actions.addAll(clearCells(region.getRight() + 1 - nCols, nCols, region.getTop(), h));
            actions.addAll(resizeMergedRegions(wgrid, startColumn, nCols, REMOVE, COLUMNS, region));
            
            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridAction removeRows(int nRows, int startRow, IGridRegion region, IWritableGrid wgrid) {
            int w = IGridRegion.Tool.width(region);
            int h = IGridRegion.Tool.height(region);
            int firstToMove = region.getTop() + startRow + nRows;

            ArrayList<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>(w * (h - startRow));

            actions.addAll(shiftRows(firstToMove, nRows, REMOVE, region));
            actions.addAll(clearCells(region.getLeft(), w, region.getBottom() + 1 - nRows, nRows));
            actions.addAll(resizeMergedRegions(wgrid, startRow, nRows, REMOVE, ROWS, region));

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

    void removeMergedRegion(IGridRegion to);

    void setCellMetaInfo(int col, int row, CellMetaInfo meta);

    void setCellStyle(int col, int row, ICellStyle style);

    void setCellValue(int col, int row, Object value);

}
