package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.actions.AUndoableCellAction;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.actions.MergeCellsAction;
import org.openl.rules.table.actions.SetBorderStyleAction;
import org.openl.rules.table.actions.UndoableClearAction;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableCopyValueAction;
import org.openl.rules.table.actions.UndoableResizeMergedRegionAction;
import org.openl.rules.table.actions.UndoableSetValueAction;
import org.openl.rules.table.actions.UndoableShiftValueAction;
import org.openl.rules.table.actions.UnmergeByColumnsAction;
import org.openl.rules.table.ui.CellStyle;

/**
 * Created by ymolchan on 8/13/2014.
 */
public class GridTool {

    private GridTool() {
        // Utility class
    }

    private static final String PROPERTIES_SECTION_NAME = "properties";
    private static final boolean COLUMNS = true;
    private static final boolean ROWS = false;
    private static final boolean INSERT = true;
    private static final boolean REMOVE = false;

    /**
     * Searches all merged regions inside the specified region of table for regions that have to be resized.
     *
     * @param grid                  Current writable grid.
     * @param firstRowOrColumn      Index of row or column for insertion/removing.
     * @param numberOfRowsOrColumns Number of elements to insert/remove.
     * @param isInsert              Flag that defines what we have to do(insert/remove).
     * @param isColumns             Flag that defines direction of insertion/removing.
     * @param regionOfTable         Region of current table.
     * @param metaInfoWriter        Needed to save meta info changes
     * @return All actions to resize merged regions.
     */
    private static List<IUndoableGridTableAction> resizeMergedRegions(IGrid grid,
                                                                      int firstRowOrColumn,
                                                                      int numberOfRowsOrColumns,
                                                                      boolean isInsert,
                                                                      boolean isColumns,
                                                                      IGridRegion regionOfTable,
                                                                      MetaInfoWriter metaInfoWriter) {
        var resizeActions = new ArrayList<IUndoableGridTableAction>();
        for (var i = 0; i < grid.getNumberOfMergedRegions(); i++) {
            var existingMergedRegion = grid.getMergedRegion(i);
            // merged region is contained by region of grid
            if (IGridRegion.Tool
                    .contains(regionOfTable, existingMergedRegion.getLeft(), existingMergedRegion.getTop())
                    && isRegionMustBeResized(existingMergedRegion,
                    firstRowOrColumn,
                    numberOfRowsOrColumns,
                    isColumns,
                    regionOfTable)) {
                var oldCellStyle = grid
                        .getCell(existingMergedRegion.getLeft(), existingMergedRegion.getBottom())
                        .getStyle();

                if (!isColumns && isInsert) {
                    for (var j = 1; j <= numberOfRowsOrColumns; j++) {
                        grid.getCell(existingMergedRegion.getLeft(), existingMergedRegion.getBottom() + 1)
                                .getStyle();
                        resizeActions.add(new SetBorderStyleAction(existingMergedRegion.getLeft(),
                                existingMergedRegion.getBottom() + j,
                                oldCellStyle,
                                metaInfoWriter));
                    }
                }

                resizeActions.add(new UndoableResizeMergedRegionAction(existingMergedRegion,
                        numberOfRowsOrColumns,
                        isInsert,
                        isColumns));
            }
        }
        return resizeActions;
    }

    /**
     * Whether the merged region grows over the lines being laid down at that place, and so comes to hold them.
     *
     * <p>A merge reaching further than what is written grows with it; one exactly as long is replaced instead —
     * if we remove all rows/columns of a region then the region must be deleted, not resized. Shared with
     * whoever must know, before a line is written, whether the table will still hold it.
     *
     * @param region                the merged region to ask about
     * @param firstRowOrColumn      the line being laid down, counted from the table's own first line
     * @param numberOfRowsOrColumns how many lines are laid down there
     * @param isColumns             whether the lines are columns rather than rows
     * @param regionOfTable         the table the lines are laid down in
     */
    public static boolean isRegionMustBeResized(IGridRegion region,
                                                 int firstRowOrColumn,
                                                 int numberOfRowsOrColumns,
                                                 boolean isColumns,
                                                 IGridRegion regionOfTable) {
        if (isColumns) {
            // merged region contains column which we copy/remove
            return IGridRegion.Tool.width(region) > numberOfRowsOrColumns && IGridRegion.Tool
                    .contains(region, regionOfTable.getLeft() + firstRowOrColumn, region.getTop());
        } else {
            // merged region contains row which we copy/remove
            return IGridRegion.Tool.height(region) > numberOfRowsOrColumns && IGridRegion.Tool
                    .contains(region, region.getLeft(), regionOfTable.getTop() + firstRowOrColumn);
        }
    }

    public static IUndoableGridTableAction insertColumns(int nCols,
                                                         int beforeColumns,
                                                         IGridRegion region,
                                                         IGrid grid,
                                                         MetaInfoWriter metaInfoWriter) {
        var h = IGridRegion.Tool.height(region);
        var w = IGridRegion.Tool.width(region);
        var columnsToMove = w - beforeColumns;

        var actions = new ArrayList<IUndoableGridTableAction>(h * columnsToMove);

        var firstToMove = region.getLeft() + beforeColumns;
        var colTo = firstToMove + nCols;
        var top = region.getTop();
        // shift cells by column, copy cells of inserted column and resize merged regions after
        actions.addAll(shiftColumns(colTo, nCols, INSERT, region, grid, metaInfoWriter));
        actions.addAll(copyCells(firstToMove, top, colTo, top, nCols, h, grid, metaInfoWriter));
        actions.addAll(resizeMergedRegions(grid, beforeColumns, nCols, INSERT, COLUMNS, region, metaInfoWriter));
        actions.addAll(emptyCells(firstToMove, top, colTo, top, nCols, h, grid, true, COLUMNS, metaInfoWriter));

        return new UndoableCompositeAction(actions);
    }

    public static IUndoableGridTableAction insertRows(int nRows,
                                                      int afterRow,
                                                      IGridRegion region,
                                                      IGrid grid,
                                                      MetaInfoWriter metaInfoWriter) {
        return insertRows(nRows, afterRow, region, grid, false, metaInfoWriter);
    }

    private static IUndoableGridTableAction insertRows(int nRows,
                                                       int row,
                                                       IGridRegion region,
                                                       IGrid grid,
                                                       boolean before,
                                                       MetaInfoWriter metaInfoWriter) {
        var h = IGridRegion.Tool.height(region);
        var w = IGridRegion.Tool.width(region);
        var rowsToMove = h - row;

        var actions = new ArrayList<IUndoableGridTableAction>(w * rowsToMove);

        var firstToMove = region.getTop() + row;
        var rowTo = firstToMove + nRows;
        var left = region.getLeft();
        // Shift cells by row, copy cells of inserted row and resize merged regions after
        actions.addAll(shiftRows(rowTo, nRows, INSERT, region, grid, metaInfoWriter));
        actions.addAll(copyCells(left, firstToMove, left, rowTo, w, nRows, grid, metaInfoWriter));
        actions.addAll(resizeMergedRegions(grid, row, nRows, INSERT, ROWS, region, metaInfoWriter));
        actions.addAll(emptyCells(left, firstToMove, left, rowTo, w, nRows, grid, before, ROWS, metaInfoWriter));

        return new UndoableCompositeAction(actions);
    }

    private static List<IUndoableGridTableAction> copyCells(int colFrom,
                                                            int rowFrom,
                                                            int colTo,
                                                            int rowTo,
                                                            int nCols,
                                                            int nRows,
                                                            IGrid grid,
                                                            MetaInfoWriter metaInfoWriter) {
        var actions = new ArrayList<IUndoableGridTableAction>();
        for (var i = nCols - 1; i >= 0; i--) {
            for (var j = nRows - 1; j >= 0; j--) {
                var cFrom = colFrom + i;
                var rFrom = rowFrom + j;
                var cTo = colTo + i;
                var rTo = rowTo + j;
                if (!grid.isInOneMergedRegion(cFrom, rFrom, cTo, rTo)) {
                    actions.add(new UndoableCopyValueAction(cFrom, rFrom, cTo, rTo, metaInfoWriter));
                }
            }
        }
        return actions;
    }

    private static List<IUndoableGridTableAction> emptyCells(int colFrom,
                                                             int rowFrom,
                                                             int colTo,
                                                             int rowTo,
                                                             int nCols,
                                                             int nRows,
                                                             IGrid grid,
                                                             boolean before,
                                                             boolean isColumns,
                                                             MetaInfoWriter metaInfoWriter) {
        var actions = new ArrayList<IUndoableGridTableAction>();
        for (var i = nCols - 1; i >= 0; i--) {
            for (var j = nRows - 1; j >= 0; j--) {
                var cFrom = colFrom + i;
                var rFrom = rowFrom + j;
                if (isColumns) {
                    if (isClearable(grid, cFrom, rFrom, nCols, nRows)) {
                        actions.add(new UndoableSetValueAction(cFrom, rFrom, null, metaInfoWriter));
                    }
                } else {
                    var cTo = colTo + i;
                    var rTo = rowTo + j;
                    if (!grid.isInOneMergedRegion(cFrom, rFrom, cTo, rTo)) {
                        actions.add(emptyCellAction(before, cFrom, rFrom, cTo, rTo, metaInfoWriter));
                    }
                }
            }
        }
        return actions;
    }

    /**
     * Creates the action that empties the source cell when cells are inserted before it, or the target cell
     * otherwise.
     */
    private static IUndoableGridTableAction emptyCellAction(boolean before,
                                                            int cFrom,
                                                            int rFrom,
                                                            int cTo,
                                                            int rTo,
                                                            MetaInfoWriter metaInfoWriter) {
        int col;
        int row;
        if (before) {
            col = cFrom;
            row = rFrom;
        } else {
            col = cTo;
            row = rTo;
        }
        return new UndoableSetValueAction(col, row, null, metaInfoWriter);
    }

    /**
     * Checks whether the cell can be cleared within a region of the given size.
     */
    private static boolean isClearable(IGrid grid, int col, int row, int nCols, int nRows) {
        if (grid.isTopLeftCellInMergedRegion(col, row)) {
            var cell = grid.getCell(col, row);
            // Don't clear merged cells which are bigger than the cleaned region.
            return cell.getHeight() <= nRows && cell.getWidth() <= nCols;
        }
        // Don't clear middle of the merged cells.
        return !grid.isPartOfTheMergedRegion(col, row);
    }

    /**
     * @return null if set new property with empty or same value
     */
    public static IUndoableGridTableAction insertProp(IGridRegion tableRegion,
                                                      IGrid grid,
                                                      String newPropName,
                                                      Object newPropValue,
                                                      MetaInfoWriter metaInfoWriter) {
        if (newPropValue == null) {
            return null;
        }

        var propertyRowIndex = getPropertyRowIndex(tableRegion, grid, newPropName);
        if (propertyRowIndex > 0) {
            return setExistingPropertyValue(tableRegion, grid, newPropValue, propertyRowIndex, metaInfoWriter);
        } else {
            return insertNewProperty(tableRegion, grid, newPropName, newPropValue, metaInfoWriter);
        }
    }

    public static int getPropertyRowIndex(IGridRegion tableRegion, IGrid grid, String newPropName) {
        var leftCell = tableRegion.getLeft();
        var topCell = tableRegion.getTop();
        var firstPropertyRow = IGridRegion.Tool.height(grid.getCell(leftCell, topCell).getAbsoluteRegion());
        var propsHeader = grid.getCell(leftCell, topCell + firstPropertyRow).getStringValue();
        if (tableWithoutPropertySection(propsHeader)) {
            return -1;
        }
        var propsCount = grid.getCell(leftCell, topCell + 1).getHeight();
        var propNameCellOffset = grid.getCell(leftCell, topCell + 1).getWidth();
        for (var i = 0; i < propsCount; i++) {
            var propNameFromTable = grid.getCell(leftCell + propNameCellOffset, topCell + 1 + i).getStringValue();
            if (propNameFromTable != null && propNameFromTable.equals(newPropName)) {
                return topCell + 1 + i;
            }
        }
        return -1;
    }

    private static IUndoableGridTableAction setExistingPropertyValue(IGridRegion tableRegion,
                                                                     IGrid grid,
                                                                     Object newPropValue,
                                                                     int propertyRowIndex,
                                                                     MetaInfoWriter metaInfoWriter) {
        var leftCell = tableRegion.getLeft();
        var topCell = tableRegion.getTop();
        var propNameCellOffset = grid.getCell(leftCell, topCell + 1).getWidth();
        var propValueCellOffset = propNameCellOffset + grid.getCell(leftCell + propNameCellOffset, topCell + 1)
                .getWidth();

        var propValueFromTable = grid.getCell(leftCell + propValueCellOffset, propertyRowIndex).getObjectValue();
        if (propValueFromTable != null && propValueFromTable.equals(newPropValue)) {
            // Property with such name and value already exists
            return null;
        }
        return new UndoableSetValueAction(leftCell + propValueCellOffset,
                propertyRowIndex,
                newPropValue,
                metaInfoWriter);
    }

    private static IUndoableGridTableAction insertNewProperty(IGridRegion tableRegion,
                                                              IGrid grid,
                                                              String newPropName,
                                                              Object newPropValue,
                                                              MetaInfoWriter metaInfoWriter) {
        var leftCell = tableRegion.getLeft();
        var topCell = tableRegion.getTop();
        var firstPropertyRow = IGridRegion.Tool.height(grid.getCell(leftCell, topCell).getAbsoluteRegion());

        var rowsToMove = IGridRegion.Tool.height(tableRegion) - firstPropertyRow;
        var actions = new ArrayList<IUndoableGridTableAction>(IGridRegion.Tool.width(tableRegion) * rowsToMove);

        var propsHeader = grid.getCell(leftCell, topCell + firstPropertyRow).getStringValue();
        int propNameCellOffset;
        int propValueCellOffset;

        if (tableWithoutPropertySection(propsHeader)) {
            actions.addAll(
                    shiftRows(tableRegion.getTop() + firstPropertyRow, 1, INSERT, tableRegion, grid, metaInfoWriter));
            actions.add(createPropertiesSection(tableRegion, grid, metaInfoWriter));
            propNameCellOffset = 1;
            propValueCellOffset = 2;
        } else {
            actions.add(insertRows(1, firstPropertyRow, tableRegion, grid, true, metaInfoWriter));
            actions.add(resizePropertiesHeader(tableRegion, grid, metaInfoWriter));
            propNameCellOffset = grid.getCell(leftCell, topCell + firstPropertyRow).getWidth();
            propValueCellOffset = propNameCellOffset + grid
                    .getCell(leftCell + propNameCellOffset, topCell + firstPropertyRow)
                    .getWidth();
        }

        actions.add(new UndoableSetValueAction(leftCell + propNameCellOffset,
                topCell + firstPropertyRow,
                newPropName,
                metaInfoWriter));

        actions.add(new UndoableSetValueAction(leftCell + propValueCellOffset,
                topCell + firstPropertyRow,
                newPropValue,
                metaInfoWriter));
        return new UndoableCompositeAction(actions);
    }

    private static IUndoableGridTableAction createPropertiesSection(IGridRegion tableRegion,
                                                                    IGrid grid,
                                                                    MetaInfoWriter metaInfoWriter) {
        var regionWidth = IGridRegion.Tool.width(tableRegion);
        var leftCell = tableRegion.getLeft();
        var topCell = tableRegion.getTop();
        var headerRegion = grid.getCell(leftCell, topCell).getAbsoluteRegion();

        var actions = new ArrayList<IUndoableGridTableAction>();

        actions.add(new SetBorderStyleAction(leftCell,
                headerRegion.getBottom() + 1,
                makeNewPropStyle(grid, leftCell, headerRegion.getBottom() + 1, leftCell, regionWidth),
                metaInfoWriter));
        actions.add(new UnmergeByColumnsAction(new GridRegion(headerRegion.getBottom() + 1,
                leftCell,
                headerRegion.getBottom() + 1,
                tableRegion.getRight())));
        actions.add(new UndoableSetValueAction(leftCell,
                headerRegion.getBottom() + 1,
                PROPERTIES_SECTION_NAME,
                metaInfoWriter));

        // clear cells for properties
        for (var prpCell = leftCell + 1; prpCell < leftCell + regionWidth; prpCell++) {
            actions.add(new UndoableClearAction(prpCell, headerRegion.getBottom() + 1, metaInfoWriter));
        }

        if (regionWidth >= 3) {
            // set cell style
            // leftCell + 2 - this is index of last property column
            for (var j = leftCell + 2; j < leftCell + regionWidth; j++) {
                actions.add(new SetBorderStyleAction(j,
                        headerRegion.getBottom() + 1,
                        makeNewPropStyle(grid, j, headerRegion.getBottom() + 1, leftCell, regionWidth),
                        metaInfoWriter));
            }
        } else {
            // expand table by including neighboring cell in merged
            // regions, width will equal 3
            var propSize = 3;

            actions
                    .add(new MergeCellsAction(new GridRegion(topCell, leftCell, headerRegion.getBottom(), leftCell + 2)));

            // add style for expanded header's and properties's cells
            for (var row = topCell; row < tableRegion.getBottom(); row++) {
                addExpandedCellsStyle(actions, grid, row, row, leftCell, regionWidth, metaInfoWriter);
            }

            // add style for expanded others cells
            for (var row = topCell + 1; row < tableRegion.getBottom(); row++) {
                addExpandedCellsStyle(actions, grid, row, row + 1, leftCell, regionWidth, metaInfoWriter);
            }

            // merge right cells in each row
            IGridRegion cellToExpandRegion;
            for (var row = headerRegion.getBottom() + 1; row < tableRegion
                    .getBottom(); row = cellToExpandRegion.getBottom() + 1) {
                cellToExpandRegion = grid.getCell(leftCell + regionWidth - 1, row).getAbsoluteRegion();

                actions.add(new MergeCellsAction(new GridRegion(row + 1,
                        cellToExpandRegion.getLeft(),
                        cellToExpandRegion.getBottom() + 1,
                        leftCell + 2)));

                actions.add(new SetBorderStyleAction(leftCell + 2,
                        topCell,
                        grid.getCell(leftCell + regionWidth - 1, topCell).getStyle(),
                        metaInfoWriter));
            }

            actions.add(new GridRegionAction(tableRegion,
                    COLUMNS,
                    INSERT,
                    GridRegionAction.ActionType.EXPAND,
                    propSize - regionWidth));
        }

        return new UndoableCompositeAction(actions);
    }

    /**
     * Styles the cells a table is expanded by in a row like the last cell of the given row of the table.
     */
    private static void addExpandedCellsStyle(List<IUndoableGridTableAction> actions,
                                              IGrid grid,
                                              int row,
                                              int targetRow,
                                              int leftCell,
                                              int regionWidth,
                                              MetaInfoWriter metaInfoWriter) {
        for (var j = leftCell + regionWidth; j < leftCell + 3; j++) {
            actions.add(new SetBorderStyleAction(j,
                    targetRow,
                    grid.getCell(leftCell + regionWidth - 1, row).getStyle(),
                    metaInfoWriter));
        }
    }

    private static CellStyle makeNewPropStyle(IGrid grid, int col, int row, int regionLeftCell, int regionWidth) {
        var cell = grid.getCell(col, row);
        var newCellStyle = new CellStyle(cell.getStyle());

        var cellStyle = cell.getStyle();
        BorderStyle[] borderStyle = cellStyle != null ? cellStyle.getBorderStyle() : null;

        /* Create new cell style */

        if (borderStyle != null && col == regionLeftCell) {
            // Only left border will be set
            if (borderStyle.length == 4) {
                borderStyle = new BorderStyle[]{BorderStyle.NONE,
                        BorderStyle.NONE,
                        BorderStyle.NONE,
                        borderStyle[3]};
            }
        } else if (borderStyle != null && col - regionLeftCell == regionWidth - 1) {
            // Only right border will be set
            if (borderStyle.length == 4) {
                borderStyle = new BorderStyle[]{BorderStyle.NONE,
                        borderStyle[1],
                        BorderStyle.NONE,
                        BorderStyle.NONE};
                // FIXME add bottom border for expender row (only for last)
            }
        } else {
            borderStyle = new BorderStyle[]{BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE};
        }

        newCellStyle.setBorderStyle(borderStyle);

        return newCellStyle;
    }

    private static IUndoableGridTableAction resizePropertiesHeader(IGridRegion tableRegion,
                                                                   IGrid grid,
                                                                   MetaInfoWriter metaInfoWriter) {
        var leftCell = tableRegion.getLeft();
        var topCell = tableRegion.getTop();
        var firstPropertyRow = IGridRegion.Tool.height(grid.getCell(leftCell, topCell).getAbsoluteRegion());

        var propsCount = grid.getCell(leftCell, topCell + firstPropertyRow).getHeight();
        if (propsCount == 1) {
            var propHeaderRegion = grid.getRegionContaining(leftCell, topCell + firstPropertyRow);
            if (propHeaderRegion == null) {
                propHeaderRegion = new GridRegion(topCell + firstPropertyRow,
                        leftCell,
                        topCell + firstPropertyRow,
                        leftCell);
            }
            return new UndoableResizeMergedRegionAction(propHeaderRegion, 1, INSERT, ROWS);
        } else {
            return new UndoableCompositeAction(
                    resizeMergedRegions(grid, firstPropertyRow, 1, INSERT, ROWS, tableRegion, metaInfoWriter));
        }

    }

    public static boolean tableWithoutPropertySection(String propsHeader) {
        var containsPropSection = false;
        if (propsHeader != null && propsHeader.equals(PROPERTIES_SECTION_NAME)) {
            containsPropSection = true;
        }
        return !containsPropSection;
    }

    private static List<IUndoableGridTableAction> clearCells(int startColumn,
                                                             int nCols,
                                                             int startRow,
                                                             int nRows,
                                                             IGrid grid,
                                                             MetaInfoWriter metaInfoWriter) {
        var clearActions = new ArrayList<IUndoableGridTableAction>();
        for (var i = startColumn; i < startColumn + nCols; i++) {
            for (var j = startRow; j < startRow + nRows; j++) {
                if (!grid.isPartOfTheMergedRegion(i, j) || grid.isTopLeftCellInMergedRegion(i, j)) {
                    clearActions.add(new UndoableClearAction(i, j, metaInfoWriter));
                }
            }
        }
        return clearActions;
    }

    private static AUndoableCellAction shiftCell(int colFrom,
                                                 int rowFrom,
                                                 int colTo,
                                                 int rowTo,
                                                 IGrid grid,
                                                 MetaInfoWriter metaInfoWriter) {

        if (!grid.isPartOfTheMergedRegion(colFrom, rowFrom) || grid.isTopLeftCellInMergedRegion(colFrom, rowFrom)) {
            // non top left cell of merged region have to be skipped
            return new UndoableShiftValueAction(colFrom, rowFrom, colTo, rowTo, metaInfoWriter);
        }

        return new SetBorderStyleAction(colTo, rowTo, grid.getCell(colFrom, rowFrom).getStyle(), false, metaInfoWriter);
    }

    private static List<IUndoableGridTableAction> shiftColumns(int startColumn,
                                                               int nCols,
                                                               boolean isInsert,
                                                               IGridRegion region,
                                                               IGrid grid,
                                                               MetaInfoWriter metaInfoWriter) {
        var shiftActions = new ArrayList<IUndoableGridTableAction>();

        // The first step: clear cells that will be lost after shifting columns
        if (isInsert) {
            shiftActions.addAll(clearCells(region.getRight() + 1,
                    nCols,
                    region.getTop(),
                    IGridRegion.Tool.height(region),
                    grid,
                    metaInfoWriter));
        } else {
            shiftActions.addAll(clearRemovedColumns(startColumn, nCols, region, grid, metaInfoWriter));
        }

        // The second step: shift cells
        int direction;
        int colFromCopy;
        int colToCopy;
        if (isInsert) {// shift columns left
            direction = -1;
            colFromCopy = region.getRight();
        } else {// shift columns right
            direction = 1;
            colFromCopy = startColumn;
        }
        var numColumnsToBeShifted = region.getRight() - startColumn;
        for (var i = 0; i <= numColumnsToBeShifted; i++) {
            colToCopy = colFromCopy - direction * nCols;
            // from bottom to top, it is made for copying non_top_left cells
            // of merged before the topleft cell of merged region
            for (var row = region.getBottom(); row >= region.getTop(); row--) {
                shiftActions.add(shiftCell(colFromCopy, row, colToCopy, row, grid, metaInfoWriter));
            }
            colFromCopy += direction;
        }
        return shiftActions;
    }

    /**
     * Clears the cells of the columns to remove. A merged cell is cleared when it is not wider than the removed
     * columns.
     */
    private static List<IUndoableGridTableAction> clearRemovedColumns(int startColumn,
                                                                      int nCols,
                                                                      IGridRegion region,
                                                                      IGrid grid,
                                                                      MetaInfoWriter metaInfoWriter) {
        var actions = new ArrayList<IUndoableGridTableAction>();
        for (var column = startColumn - nCols; column < startColumn; column++) {
            for (var row = region.getTop(); row <= region.getBottom(); row++) {
                if (!grid.isPartOfTheMergedRegion(column, row) || grid.isTopLeftCellInMergedRegion(column,
                        row) && IGridRegion.Tool.width(grid.getRegionStartingAt(column, row)) <= nCols) {
                    // Sense of the second check: if it was a merged
                    // cell then it can be removed or resized depending
                    // on count of columns deleted
                    actions.add(new UndoableClearAction(column, row, metaInfoWriter));
                }
            }
        }
        return actions;
    }

    /**
     * @param startRow       number of the row in region to start some manipulations (shifting down or up)
     * @param nRows          number of rows to be moved
     * @param isInsert       do we need to insert rows or to shift it up.
     * @param region         region to work with.
     * @param metaInfoWriter class needed to save meta info modifications
     */
    private static List<IUndoableGridTableAction> shiftRows(int startRow,
                                                            int nRows,
                                                            boolean isInsert,
                                                            IGridRegion region,
                                                            IGrid grid,
                                                            MetaInfoWriter metaInfoWriter) {
        var shiftActions = new ArrayList<IUndoableGridTableAction>();

        // The first step: clear cells that will be lost after shifting rows
        if (isInsert) {
            shiftActions.addAll(clearCells(region
                    .getLeft(), IGridRegion.Tool.width(region), region.getBottom() + 1, nRows, grid, metaInfoWriter));
        } else {
            shiftActions.addAll(clearRemovedRows(startRow, nRows, region, grid, metaInfoWriter));
        }

        // The second step: shift cells
        int direction;
        int rowFromCopy;
        if (isInsert) {// shift rows down
            direction = -1;
            rowFromCopy = region.getBottom(); // we gets the bottom row from the region, and are
            // going to shift it down.
        } else {// shift rows up
            direction = 1;
            rowFromCopy = startRow; // we gets the startRow and are
            // going to shift it up.
        }
        var numRowsToBeShifted = region.getBottom() - startRow;
        for (var i = 0; i <= numRowsToBeShifted; i++) {
            var rowToCopy = rowFromCopy - direction * nRows; // compute to which row we need to shift.
            // from right to left, it is made for copying non_top_left cells
            // of merged before the topleft cell of merged region
            for (var column = region.getRight(); column >= region.getLeft(); column--) {
                shiftActions.add(shiftCell(column, rowFromCopy, column, rowToCopy, grid, metaInfoWriter));
            }
            rowFromCopy += direction;
        }
        return shiftActions;
    }

    /**
     * Clears the cells of the rows to remove. A merged cell is cleared when it is not higher than the removed rows.
     */
    private static List<IUndoableGridTableAction> clearRemovedRows(int startRow,
                                                                   int nRows,
                                                                   IGridRegion region,
                                                                   IGrid grid,
                                                                   MetaInfoWriter metaInfoWriter) {
        var actions = new ArrayList<IUndoableGridTableAction>();
        for (var row = startRow - nRows; row < startRow; row++) {
            for (var column = region.getLeft(); column <= region.getRight(); column++) {
                if (!grid.isPartOfTheMergedRegion(column, row) || grid.isTopLeftCellInMergedRegion(column,
                        row) && IGridRegion.Tool.height(grid.getRegionStartingAt(column, row)) <= nRows) {
                    // Sense of the second check: if it was a merged
                    // cell then it can be removed or resized depending
                    // on count of rows deleted
                    actions.add(new UndoableClearAction(column, row, metaInfoWriter));
                }
            }
        }
        return actions;
    }

    public static IUndoableGridTableAction removeColumns(int nCols,
                                                         int startColumn,
                                                         IGridRegion region,
                                                         IGrid grid,
                                                         MetaInfoWriter metaInfoWriter) {
        var firstToMove = region.getLeft() + startColumn + nCols;
        var w = IGridRegion.Tool.width(region);
        var h = IGridRegion.Tool.height(region);

        var actions = new ArrayList<IUndoableGridTableAction>(h * (w - startColumn));

        // resize merged regions -> shift cells by column -> clear cells
        actions.addAll(resizeMergedRegions(grid, startColumn, nCols, REMOVE, COLUMNS, region, metaInfoWriter));
        actions.addAll(shiftColumns(firstToMove, nCols, REMOVE, region, grid, metaInfoWriter));
        actions.addAll(clearCells(region.getRight() + 1 - nCols, nCols, region.getTop(), h, grid, metaInfoWriter));

        return new UndoableCompositeAction(actions);
    }

    public static IUndoableGridTableAction removeRows(int nRows,
                                                      int startRow,
                                                      IGridRegion region,
                                                      IGrid grid,
                                                      MetaInfoWriter metaInfoWriter) {
        var w = IGridRegion.Tool.width(region);
        var h = IGridRegion.Tool.height(region);
        var firstToMove = region.getTop() + startRow + nRows;

        var actions = new ArrayList<IUndoableGridTableAction>(w * (h - startRow));

        // resize merged regions -> shift cells by row -> clear cells
        actions.addAll(resizeMergedRegions(grid, startRow, nRows, REMOVE, ROWS, region, metaInfoWriter));
        actions.addAll(shiftRows(firstToMove, nRows, REMOVE, region, grid, metaInfoWriter));
        actions.addAll(clearCells(region.getLeft(), w, region.getBottom() + 1 - nRows, nRows, grid, metaInfoWriter));

        return new UndoableCompositeAction(actions);
    }
}
