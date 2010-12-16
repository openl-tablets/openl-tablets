/**
 * Created Feb 15, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.actions.AUndoableCellAction;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.actions.MergeCellsAction;
import org.openl.rules.table.actions.UndoableClearAction;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableCopyValueAction;
import org.openl.rules.table.actions.UndoableResizeMergedRegionAction;
import org.openl.rules.table.actions.UndoableSetValueAction;
import org.openl.rules.table.actions.UndoableShiftValueAction;
import org.openl.rules.table.actions.UnmergeByColumnsAction;
import org.openl.rules.table.actions.GridRegionAction.ActionType;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridExporter;
import org.openl.rules.table.xls.XlsSheetGridHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.XlsFormattersManager;

import org.openl.util.export.IExporter;
import org.openl.util.formatters.IFormatter;
import org.apache.commons.lang.StringUtils;


/**
 * @author snshor
 * 
 */
public interface IWritableGrid extends IGrid {

    class Tool {

        private static final String PROPERTIES_SECTION_NAME = "properties";
        static final boolean COLUMNS = true, ROWS = false, INSERT = true, REMOVE = false;        
        
        @Deprecated
        public static IExporter createExporter(IWritableGrid wGrid) {
            if (wGrid instanceof XlsSheetGridModel) {
                return new XlsSheetGridExporter((XlsSheetGridModel) wGrid);
            }

            return null;
        }
        
        /**
         * Is deprecated, because of incorrect location. Use 
         * {@link XlsSheetGridHelper#createExporter(XlsWorkbookSourceCodeModule)}
         * 
         * @param workbookModule
         * @return
         */
        @Deprecated
        public static IExporter createExporter(XlsWorkbookSourceCodeModule workbookModule) {
            return XlsSheetGridHelper.createExporter(workbookModule);
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
        public static List<IUndoableGridTableAction> resizeMergedRegions(IGridTable table, int firstRowOrColumn,
                int numberOfRowsOrColumns, boolean isInsert, boolean isColumns, IGridRegion regionOfTable) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            ArrayList<IUndoableGridTableAction> resizeActions = new ArrayList<IUndoableGridTableAction>();
            for (int i = 0; i < grid.getNumberOfMergedRegions(); i++) {
                IGridRegion existingMergedRegion = grid.getMergedRegion(i);
                // merged region is contained by region of grid
                if (IGridRegion.Tool.contains(regionOfTable, existingMergedRegion.getLeft(), existingMergedRegion
                        .getTop())) {
                    if (isRegionMustBeResized(existingMergedRegion, firstRowOrColumn, numberOfRowsOrColumns, isColumns,
                            regionOfTable)) {
                        resizeActions.add(new UndoableResizeMergedRegionAction(existingMergedRegion,
                                numberOfRowsOrColumns, isInsert, isColumns));
                    }
                }
            }
            return resizeActions;
        }

        /**
         * Checks if the specified region must be resized.
         * 
         * If we delete all we remove all rows/columns in region then region
         * must be deleted(not resized).
         */
        private static boolean isRegionMustBeResized(IGridRegion region, int firstRowOrColumn,
                int numberOfRowsOrColumns, boolean isColumns, IGridRegion regionOfTable) {
            if (isColumns) {
                // merged region contains column which we copy/remove
                if (IGridRegion.Tool.width(region) > numberOfRowsOrColumns
                        && IGridRegion.Tool.contains(region, regionOfTable.getLeft() + firstRowOrColumn, region
                                .getTop())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // merged region contains row which we copy/remove
                if (IGridRegion.Tool.height(region) > numberOfRowsOrColumns
                        && IGridRegion.Tool.contains(region, region.getLeft(), regionOfTable.getTop()
                                + firstRowOrColumn)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        public static IUndoableGridTableAction insertColumns(int nColumns, int beforeColumns, IGridRegion region,
                IGridTable table) {
            int h = IGridRegion.Tool.height(region);
            int w = IGridRegion.Tool.width(region);
            int columnsToMove = w - beforeColumns;

            ArrayList<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>(h * columnsToMove);

            int firstToMove = region.getLeft() + beforeColumns;
            // shift cells by column, copy cells of inserted column and resize merged regions after
            actions.addAll(shiftColumns(firstToMove + nColumns, nColumns, INSERT, region, table));

            for (int colFromCopy = firstToMove + nColumns - 1; colFromCopy >= firstToMove; colFromCopy--) {
                for (int row = region.getBottom(); row >= region.getTop(); row--) {
                    AUndoableCellAction action = copyCell(colFromCopy, row, colFromCopy + nColumns, row, table);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }

            actions.addAll(resizeMergedRegions(table, beforeColumns, nColumns, INSERT, COLUMNS, region));

            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridTableAction insertRows(int nRows, int beforeRow,
                IGridRegion region, IGridTable table) {
            int h = IGridRegion.Tool.height(region);
            int w = IGridRegion.Tool.width(region);
            int rowsToMove = h - beforeRow;

            ArrayList<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>(w * rowsToMove);

            int firstToMove = region.getTop() + beforeRow;
            // shift cells by row, copy cells of inserted row and resize merged regions after
            actions.addAll(shiftRows(firstToMove + nRows, nRows, INSERT, region, table));
            for (int rowFromCopy = firstToMove + nRows - 1; rowFromCopy >= firstToMove; rowFromCopy--) {
                for (int column = region.getRight(); column >= region.getLeft(); column--) {
                    IUndoableGridTableAction action = copyCell(column, rowFromCopy, column, rowFromCopy + nRows, table);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
            actions.addAll(resizeMergedRegions(table, beforeRow, nRows, INSERT, ROWS, region));

            return new UndoableCompositeAction(actions);
        }

        /**
         * Checks if the table specified by its region contains property.
         */
        public static CellKey getPropertyCoordinates(IGridRegion region, IGridTable table, String propName) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            int left = region.getLeft();
            int top = region.getTop();

            ICell propsHeaderCell = grid.getCell(left, top + 1);
            String propsHeader = propsHeaderCell.getStringValue();
            if (propsHeader == null || !propsHeader.equals(PROPERTIES_SECTION_NAME)) {
                // There is no properties
                return null;
            }
            int propsCount = propsHeaderCell.getHeight();

            for (int i = 0; i < propsCount; i++) {
                ICell propNameCell = grid.getCell(left + propsHeaderCell.getWidth(), top + 1 + i);
                String pName = propNameCell.getStringValue();

                if (pName != null && pName.equals(propName)) {
                    return new CellKey(1, 1 + i);
                }
            }

            return null;
        }

        /**
         * @return null if set new property with empty or same value
         */
        public static IUndoableGridTableAction insertProp(IGridRegion tableRegion, IGridTable table,
                String newPropName, String newPropValue) {
            if (StringUtils.isBlank(newPropValue)) {
                return null;
            }

            int propertyRowIndex = getPropertyRowIndex(tableRegion, table, newPropName);
            if (propertyRowIndex > 0) {
                return setExistingPropertyValue(tableRegion, table, newPropName, newPropValue, propertyRowIndex);
            } else {
                return insertNewProperty(tableRegion, table, newPropName, newPropValue);
            }
        }

        private static int getPropertyRowIndex(IGridRegion tableRegion, IGridTable table, String newPropName) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            int leftCell = tableRegion.getLeft();
            int topCell = tableRegion.getTop();
            String propsHeader = grid.getCell(leftCell, topCell + 1).getStringValue();
            if (!tableContainsPropertySection(propsHeader)) {
                return -1;
            }
            int propsCount = grid.getCell(leftCell, topCell + 1).getHeight();
            int propNameCellOffset = grid.getCell(leftCell, topCell + 1).getWidth();
            for (int i = 0; i < propsCount; i++) {
                String propNameFromTable = grid.getCell(leftCell + propNameCellOffset, topCell + 1 + i)
                        .getStringValue();
                if (propNameFromTable != null && propNameFromTable.equals(newPropName)) {
                    return topCell + 1 + i;
                }
            }
            return -1;
        }
        
        private static IUndoableGridTableAction setExistingPropertyValue(IGridRegion tableRegion, IGridTable table,
                String newPropName, String newPropValue, int propertyRowIndex) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            int leftCell = tableRegion.getLeft();
            int topCell = tableRegion.getTop();
            int propNameCellOffset = grid.getCell(leftCell, topCell + 1).getWidth();
            int propValueCellOffset = propNameCellOffset
                    + grid.getCell(leftCell + propNameCellOffset, topCell + 1).getWidth();

            String propValueFromTable = grid.getCell(leftCell + propValueCellOffset, propertyRowIndex)
                    .getStringValue();
            if (propValueFromTable != null && newPropValue != null
                    && propValueFromTable.trim().equals(newPropValue.trim())) {
                // property with such name and value already exists.
                return null;
            }
            IFormatter format = getFormat(newPropName);
            return new UndoableSetValueAction(leftCell + propValueCellOffset, propertyRowIndex, newPropValue, format);
        }
        
        private static IUndoableGridTableAction insertNewProperty(IGridRegion tableRegion,
                IGridTable table, String newPropName, String newPropValue) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            IFormatter format = getFormat(newPropName);
            int leftCell = tableRegion.getLeft();
            int topCell = tableRegion.getTop();
            int firstPropertyRow = IGridRegion.Tool.height(grid.getCell(leftCell, topCell).getAbsoluteRegion());

            int rowsToMove = IGridRegion.Tool.height(tableRegion) - firstPropertyRow;
            ArrayList<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>(IGridRegion.Tool
                    .width(tableRegion)* rowsToMove);

            String propsHeader = grid.getCell(leftCell, topCell + 1).getStringValue();
            int propNameCellOffset;
            int propValueCellOffset;

            if (!tableContainsPropertySection(propsHeader)) {
                actions.addAll(shiftRows(tableRegion.getTop() + firstPropertyRow, 1, INSERT, tableRegion, table));
                actions.add(createPropertiesSection(tableRegion, table));
                propNameCellOffset = 1;
                propValueCellOffset = 2;
            } else {
                actions.add(insertRows(1, firstPropertyRow, tableRegion, table));
                actions.add(resizePropertiesHeader(tableRegion, table));
                propNameCellOffset = grid.getCell(leftCell, topCell + 1).getWidth();
                propValueCellOffset = propNameCellOffset
                        + grid.getCell(leftCell + propNameCellOffset, topCell + 1).getWidth();
            }
            
            actions.add(new UndoableSetValueAction(leftCell + propNameCellOffset, topCell + firstPropertyRow,
                    newPropName, null));
            actions.add(new UndoableSetValueAction(leftCell + propValueCellOffset, topCell + firstPropertyRow,
                    newPropValue, format));
            return new UndoableCompositeAction(actions);
        }
        
        private static IUndoableGridTableAction createPropertiesSection(IGridRegion tableRegion, IGridTable table) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            int regionWidth = IGridRegion.Tool.width(tableRegion);
            int leftCell = tableRegion.getLeft();
            int topCell = tableRegion.getTop();
            IGridRegion headerRegion = grid.getCell(leftCell, topCell).getAbsoluteRegion();

            ArrayList<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>();
            actions.add(new UnmergeByColumnsAction(new GridRegion(headerRegion.getBottom() + 1, leftCell, headerRegion
                    .getBottom() + 1, tableRegion.getRight())));
            actions.add(new UndoableSetValueAction(leftCell, headerRegion.getBottom() + 1, PROPERTIES_SECTION_NAME,
                    null));
            if (regionWidth > 3) {
                // clear cells
                for (int j = leftCell + 3; j < leftCell + regionWidth; j++) {
                    actions.add(new UndoableClearAction(j, headerRegion.getBottom() + 1));
                }
            } else if (regionWidth < 3) {
                // expand table by including neighboring cell in merged
                // regions, width will equal 3
                actions.add(new MergeCellsAction(new GridRegion(topCell, leftCell, headerRegion.getBottom(),
                        leftCell + 2)));

                // merge right cells in each row
                IGridRegion cellToExpandRegion;
                for (int row = headerRegion.getBottom() + 1; row < tableRegion.getBottom(); row = cellToExpandRegion
                        .getBottom() + 1) {
                    cellToExpandRegion = grid.getCell(leftCell + regionWidth - 1, row).getAbsoluteRegion();
                    actions.add(new MergeCellsAction(new GridRegion(row + 1, cellToExpandRegion.getLeft(),
                            cellToExpandRegion.getBottom() + 1, leftCell + 2)));
                }

                actions.add(new GridRegionAction(tableRegion, COLUMNS, INSERT, ActionType.EXPAND, 3 - regionWidth));
            }
            return new UndoableCompositeAction(actions);
        }

        private static IUndoableGridTableAction resizePropertiesHeader(IGridRegion tableRegion, IGridTable table) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            int firstPropertyRow = 1;
            int leftCell = tableRegion.getLeft();
            int topCell = tableRegion.getTop();

            int propsCount = grid.getCell(leftCell, topCell + 1).getHeight();
            if (propsCount == 1) {
                IGridRegion propHeaderRegion = grid.getRegionContaining(leftCell, topCell + 1);
                if (propHeaderRegion == null) {
                    propHeaderRegion = new GridRegion(topCell + 1, leftCell, topCell + 1, leftCell);
                }
                return new UndoableResizeMergedRegionAction(propHeaderRegion, 1, INSERT, ROWS);
            } else {
                return new UndoableCompositeAction(resizeMergedRegions(table, firstPropertyRow, 1, INSERT, ROWS, tableRegion));
            }

        }

        private static boolean tableContainsPropertySection(String propsHeader) {
            boolean containsPropSection = false;
            if (propsHeader != null && propsHeader.equals(PROPERTIES_SECTION_NAME)) {
                containsPropSection = true;
            }
            return containsPropSection;
        }

        private static IFormatter getFormat(String propertyName) {

            IFormatter result = null;
            TablePropertyDefinition tablePropeprtyDefinition = TablePropertyDefinitionUtils
                    .getPropertyByName(propertyName);

            if (tablePropeprtyDefinition != null) {

                Class<?> type = tablePropeprtyDefinition.getType().getInstanceClass();
                result = XlsFormattersManager.getFormatter(type, tablePropeprtyDefinition.getFormat());
            }

            return result;
        }

        private static List<IUndoableGridTableAction> clearCells(int startColumn, int nCols, int startRow, int nRows, IGrid grid) {
            ArrayList<IUndoableGridTableAction> clearActions = new ArrayList<IUndoableGridTableAction>();
            for (int i = startColumn; i < startColumn + nCols; i++) {
                for (int j = startRow; j < startRow + nRows; j++) {
                    if (!grid.isPartOfTheMergedRegion(i, j)
                            || (grid.isTopLeftCellInMergedRegion(i, j))){
                    clearActions.add(new UndoableClearAction(i, j));
                }
                }
            }
            return clearActions;
        }

        private static AUndoableCellAction shiftCell(int colFrom, int rowFrom, int colTo, int rowTo, IGridTable table) {
            IGrid grid = table.getGrid();
            if (!grid.isPartOfTheMergedRegion(colFrom, rowFrom) || grid.isTopLeftCellInMergedRegion(colFrom, rowFrom)) {
                // non top left cell of merged region have to be skipped
                return new UndoableShiftValueAction(colFrom, rowFrom, colTo, rowTo);
            }
            return null;
        }

        private static AUndoableCellAction copyCell(int colFrom, int rowFrom, int colTo, int rowTo, IGridTable table) {
            IWritableGrid grid = (IWritableGrid) table.getGrid();
            if (!grid.isInOneMergedRegion(colFrom, rowFrom, colTo, rowTo)) {
                return new UndoableCopyValueAction(colFrom, rowFrom, colTo, rowTo);
            }
            return null;
        }

        private static List<IUndoableGridTableAction> shiftColumns(int startColumn, int nCols, boolean isInsert,
                IGridRegion region, IGridTable table) {
            ArrayList<IUndoableGridTableAction> shiftActions = new ArrayList<IUndoableGridTableAction>();
            int direction, colFromCopy, colToCopy;
            if (isInsert) {// shift columns left
                direction = -1;
                colFromCopy = region.getRight();
            } else {// shift columns right
                direction = 1;
                colFromCopy = startColumn;
            }
            IGrid grid = table.getGrid();

            // The first step: clear cells that will be lost after shifting
            // columns(just because we need to restore this cells after UNDO)
            if (isInsert) {
                shiftActions.addAll(clearCells(region.getRight() + 1, nCols, region.getTop(),
                        org.openl.rules.table.IGridRegion.Tool.height(region), grid));
            } else {
                for (int column = startColumn - nCols; column < startColumn; column++) {
                    for (int row = region.getTop(); row <= region.getBottom(); row++) {
                        if (!grid.isPartOfTheMergedRegion(column, row)
                                || (grid.isTopLeftCellInMergedRegion(column, row) && org.openl.rules.table.IGridRegion.Tool
                                        .width(grid.getRegionStartingAt(column, row)) <= nCols)) {
                            // Sense of the second check: if it was a merged
                            // cell then it can be removed or resized depending
                            // on count of columns deleted
                            shiftActions.add(new UndoableClearAction(column, row));
                        }
                    }
                }
            }

            //The second step: shift cells
            int numColumnsToBeShifted = region.getRight() - startColumn;
            for (int i = 0; i <= numColumnsToBeShifted; i++) {
                colToCopy = colFromCopy - direction * nCols;
                // from bottom to top, it is made for copying non_top_left cells
                // of merged before the topleft cell of merged region
                for (int row = region.getBottom(); row >= region.getTop(); row--) {
                    AUndoableCellAction action =  shiftCell(colFromCopy, row, colToCopy, row, table);
                    if (action != null) {
                        shiftActions.add(action);
                    }
                }
                colFromCopy += direction;
            }
            return shiftActions;
        }
        
        /**
         * 
         * @param startRow number of the row in region to start some manipulations (shifting down or up) 
         * @param nRows number of rows to be moved
         * @param isInsert do we need to insert rows or to shift it up. 
         * @param region region to work with.
         * @return
         */
        private static List<IUndoableGridTableAction> shiftRows(int startRow, int nRows, boolean isInsert,
                IGridRegion region, IGridTable table) {
            ArrayList<IUndoableGridTableAction> shiftActions = new ArrayList<IUndoableGridTableAction>();
            int direction, rowFromCopy, rowToCopy;
            if (isInsert) {// shift rows down
                direction = -1;                
                rowFromCopy = region.getBottom(); // we gets the bottom row from the region, and are
                                                  // going to shift it down.
            } else {// shift rows up
                direction = 1;
                rowFromCopy = startRow; // we gets the startRow and are
                                        // going to shift it up.
            }
            IGrid grid = table.getGrid();

            // The first step: clear cells that will be lost after shifting
            // rows(just because we need to restore this cells after UNDO)
            if (isInsert) {
                shiftActions.addAll(clearCells(region.getLeft(), org.openl.rules.table.IGridRegion.Tool.width(region),
                        region.getBottom() + 1, nRows, grid));
            } else {
                for (int row = startRow - nRows; row < startRow; row++) {
                    for (int column = region.getLeft(); column <= region.getRight(); column++) {
                        if (!grid.isPartOfTheMergedRegion(column, row)
                                || (grid.isTopLeftCellInMergedRegion(column, row) && org.openl.rules.table.IGridRegion.Tool
                                        .height(grid.getRegionStartingAt(column, row)) <= nRows)) {
                            // Sense of the second check: if it was a merged
                            // cell then it can be removed or resized depending
                            // on count of rows deleted
                            shiftActions.add(new UndoableClearAction(column, row));
                        }
                    }
                }
            }
            
            //The second step: shift cells
            int numRowsToBeShifted = region.getBottom() - startRow;
            for (int i = 0; i <= numRowsToBeShifted; i++) {
                rowToCopy = rowFromCopy - direction * nRows; // compute to which row we need to shift.
                // from right to left, it is made for copying non_top_left cells
                // of merged before the topleft cell of merged region
                for (int column = region.getRight(); column >= region.getLeft(); column--) {
                    AUndoableCellAction action = shiftCell(column, rowFromCopy, column, rowToCopy, table);
                    if (action != null) {
                        shiftActions.add(action);
                    }
                }
                rowFromCopy += direction;
            }
            return shiftActions;
        }

        public static IUndoableGridTableAction removeColumns(int nCols, int startColumn, IGridRegion region,
                IGridTable table) {
            int firstToMove = region.getLeft() + startColumn + nCols;
            int w = IGridRegion.Tool.width(region);
            int h = IGridRegion.Tool.height(region);

            ArrayList<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>(h * (w - startColumn));

            // resize merged regions -> shift cells by column -> clear cells 
            actions.addAll(resizeMergedRegions(table, startColumn, nCols, REMOVE, COLUMNS, region));
            actions.addAll(shiftColumns(firstToMove, nCols, REMOVE, region, table));
            actions.addAll(clearCells(region.getRight() + 1 - nCols, nCols, region.getTop(), h, table.getGrid()));

            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridTableAction removeRows(int nRows, int startRow,
                IGridRegion region, IGridTable table) {
            int w = IGridRegion.Tool.width(region);
            int h = IGridRegion.Tool.height(region);
            int firstToMove = region.getTop() + startRow + nRows;
            
            ArrayList<IUndoableGridTableAction> actions = new ArrayList<IUndoableGridTableAction>(w * (h - startRow));

            // resize merged regions -> shift cells by row -> clear cells 
            actions.addAll(resizeMergedRegions(table, startRow, nRows, REMOVE, ROWS, region));
            actions.addAll(shiftRows(firstToMove, nRows, REMOVE, region, table));
            actions.addAll(clearCells(region.getLeft(), w, region.getBottom() + 1 - nRows, nRows, table.getGrid()));

            return new UndoableCompositeAction(actions);
        }

        public static IUndoableGridTableAction setStringValue(int col, int row, IGridRegion region, String value,
                IFormatter format) {
            int gcol = region.getLeft() + col;
            int grow = region.getTop() + row;

            // wgrid.setCellStringValue(gcol, grow, value);
            return new UndoableSetValueAction(gcol, grow, value, format);
        }

        public static IUndoableGridTableAction setStringValue(int col, int row, IGridTable table, String value,
                IFormatter format) {
            // IWritableGrid wgrid = getWritableGrid(table);
            int gcol = table.getGridColumn(col, row);
            int grow = table.getGridRow(col, row);

            // wgrid.setCellStringValue(gcol, grow, value);
            return new UndoableSetValueAction(gcol, grow, value, format);

        }

    }

    int addMergedRegion(IGridRegion reg);

    void clearCell(int col, int row);

    void createCell(int col, int row, Object value, String formula, ICellStyle style, ICellComment comment);

    void copyCell(int colFrom, int rowFrom, int colTo, int rowTo);

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

    void removeMergedRegion(IGridRegion to);

    void setCellMetaInfo(int col, int row, CellMetaInfo meta);

    void setCellStyle(int col, int row, ICellStyle style);

    void setCellAlignment(int col, int row, int alignment);

    void setCellIndent(int col, int row, int indent);

    void setCellFillColor(int col, int row, short[] color);

    void setCellFontBold(int col, int row, boolean bold);

    void setCellFontItalic(int col, int row, boolean italic);

    void setCellFontUnderline(int col, int row, boolean underlined);

    void setCellFontColor(int col, int row, short[] color);

    void setCellComment(int col, int row, ICellComment comment);

    void setCellValue(int col, int row, Object value);

    void setCellFormula(int col, int row, String formula);
}
