package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.actions.*;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;

/**
 * Created by ymolchan on 8/13/2014.
 */
public class GridTool {

    private static final String PROPERTIES_SECTION_NAME = "properties";
    private static final boolean COLUMNS = true, ROWS = false, INSERT = true, REMOVE = false;

    /**
     * Searches all merged regions inside the specified region of table for regions that have to be resized.
     *
     * @param grid Current writable grid.
     * @param firstRowOrColumn Index of row or column for insertion/removing.
     * @param numberOfRowsOrColumns Number of elements to insert/remove.
     * @param isInsert Flag that defines what we have to do(insert/remove).
     * @param isColumns Flag that defines direction of insertion/removing.
     * @param regionOfTable Region of current table.
     * @param metaInfoWriter Needed to save meta info changes
     * @return All actions to resize merged regions.
     */
    private static List<IUndoableGridTableAction> resizeMergedRegions(IGrid grid,
            int firstRowOrColumn,
            int numberOfRowsOrColumns,
            boolean isInsert,
            boolean isColumns,
            IGridRegion regionOfTable,
            MetaInfoWriter metaInfoWriter) {
        ArrayList<IUndoableGridTableAction> resizeActions = new ArrayList<>();
        for (int i = 0; i < grid.getNumberOfMergedRegions(); i++) {
            IGridRegion existingMergedRegion = grid.getMergedRegion(i);
            // merged region is contained by region of grid
            if (IGridRegion.Tool
                .contains(regionOfTable, existingMergedRegion.getLeft(), existingMergedRegion.getTop())) {
                if (isRegionMustBeResized(existingMergedRegion,
                    firstRowOrColumn,
                    numberOfRowsOrColumns,
                    isColumns,
                    regionOfTable)) {
                    ICellStyle oldCellStyle = grid
                        .getCell(existingMergedRegion.getLeft(), existingMergedRegion.getBottom())
                        .getStyle();

                    if (!isColumns && isInsert) {
                        for (int j = 1; j <= numberOfRowsOrColumns; j++) {
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
        }
        return resizeActions;
    }

    /**
     * Checks if the specified region must be resized.
     *
     * If we delete all we remove all rows/columns in region then region must be deleted(not resized).
     */
    private static boolean isRegionMustBeResized(IGridRegion region,
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
        int h = IGridRegion.Tool.height(region);
        int w = IGridRegion.Tool.width(region);
        int columnsToMove = w - beforeColumns;

        ArrayList<IUndoableGridTableAction> actions = new ArrayList<>(h * columnsToMove);

        int firstToMove = region.getLeft() + beforeColumns;
        int colTo = firstToMove + nCols;
        int top = region.getTop();
        // shift cells by column, copy cells of inserted column and resize merged regions after
        actions.addAll(shiftColumns(colTo, nCols, INSERT, region, grid, metaInfoWriter));
        actions.addAll(copyCells(firstToMove, top, colTo, top, nCols, h, grid, metaInfoWriter));
        actions.addAll(resizeMergedRegions(grid, beforeColumns, nCols, INSERT, COLUMNS, region, metaInfoWriter));
        actions.addAll(emptyCells(firstToMove, top, nCols, h, grid, metaInfoWriter));

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
        int h = IGridRegion.Tool.height(region);
        int w = IGridRegion.Tool.width(region);
        int rowsToMove = h - row;

        ArrayList<IUndoableGridTableAction> actions = new ArrayList<>(w * rowsToMove);

        int firstToMove = region.getTop() + row;
        int rowTo = firstToMove + nRows;
        int left = region.getLeft();
        // Shift cells by row, copy cells of inserted row and resize merged regions after
        actions.addAll(shiftRows(rowTo, nRows, INSERT, region, grid, metaInfoWriter));
        actions.addAll(copyCells(left, firstToMove, left, rowTo, w, nRows, grid, metaInfoWriter));
        actions.addAll(resizeMergedRegions(grid, row, nRows, INSERT, ROWS, region, metaInfoWriter));

        int rowFrom = before ? firstToMove : rowTo;
        actions.addAll(emptyCells(left, rowFrom, w, nRows, grid, metaInfoWriter));

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
        List<IUndoableGridTableAction> actions = new ArrayList<>();
        for (int i = nCols - 1; i >= 0; i--) {
            for (int j = nRows - 1; j >= 0; j--) {
                int cFrom = colFrom + i;
                int rFrom = rowFrom + j;
                int cTo = colTo + i;
                int rTo = rowTo + j;
                if (!grid.isInOneMergedRegion(cFrom, rFrom, cTo, rTo)) {
                    actions.add(new UndoableCopyValueAction(cFrom, rFrom, cTo, rTo, metaInfoWriter));
                }
            }
        }
        return actions;
    }

    private static List<IUndoableGridTableAction> emptyCells(int colFrom,
            int rowFrom,
            int nCols,
            int nRows,
            IGrid grid,
            MetaInfoWriter metaInfoWriter) {
        List<IUndoableGridTableAction> actions = new ArrayList<>();
        for (int i = nCols - 1; i >= 0; i--) {
            for (int j = nRows - 1; j >= 0; j--) {
                int cFrom = colFrom + i;
                int rFrom = rowFrom + j;
                if (grid.isTopLeftCellInMergedRegion(cFrom, rFrom)) {
                    ICell cell = grid.getCell(cFrom, rFrom);
                    if (cell.getHeight() > nRows || cell.getWidth() > nCols) {
                        // Don't clear merged cells which are bigger than the cleaned region.
                        continue;
                    }
                } else if (grid.isPartOfTheMergedRegion(cFrom, rFrom)) {
                    // Don't clear middle of the merged cells.
                    continue;
                }
                actions.add(new UndoableSetValueAction(cFrom, rFrom, null, metaInfoWriter));
            }
        }
        return actions;
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

        int propertyRowIndex = getPropertyRowIndex(tableRegion, grid, newPropName);
        if (propertyRowIndex > 0) {
            return setExistingPropertyValue(tableRegion, grid, newPropValue, propertyRowIndex, metaInfoWriter);
        } else {
            return insertNewProperty(tableRegion, grid, newPropName, newPropValue, metaInfoWriter);
        }
    }

    private static int getPropertyRowIndex(IGridRegion tableRegion, IGrid grid, String newPropName) {
        int leftCell = tableRegion.getLeft();
        int topCell = tableRegion.getTop();
        int firstPropertyRow = IGridRegion.Tool.height(grid.getCell(leftCell, topCell).getAbsoluteRegion());
        String propsHeader = grid.getCell(leftCell, topCell + firstPropertyRow).getStringValue();
        if (tableWithoutPropertySection(propsHeader)) {
            return -1;
        }
        int propsCount = grid.getCell(leftCell, topCell + 1).getHeight();
        int propNameCellOffset = grid.getCell(leftCell, topCell + 1).getWidth();
        for (int i = 0; i < propsCount; i++) {
            String propNameFromTable = grid.getCell(leftCell + propNameCellOffset, topCell + 1 + i).getStringValue();
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
        int leftCell = tableRegion.getLeft();
        int topCell = tableRegion.getTop();
        int propNameCellOffset = grid.getCell(leftCell, topCell + 1).getWidth();
        int propValueCellOffset = propNameCellOffset + grid.getCell(leftCell + propNameCellOffset, topCell + 1)
            .getWidth();

        Object propValueFromTable = grid.getCell(leftCell + propValueCellOffset, propertyRowIndex).getObjectValue();
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
        int leftCell = tableRegion.getLeft();
        int topCell = tableRegion.getTop();
        int firstPropertyRow = IGridRegion.Tool.height(grid.getCell(leftCell, topCell).getAbsoluteRegion());

        int rowsToMove = IGridRegion.Tool.height(tableRegion) - firstPropertyRow;
        ArrayList<IUndoableGridTableAction> actions = new ArrayList<>(IGridRegion.Tool.width(tableRegion) * rowsToMove);

        String propsHeader = grid.getCell(leftCell, topCell + firstPropertyRow).getStringValue();
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
        int regionWidth = IGridRegion.Tool.width(tableRegion);
        int leftCell = tableRegion.getLeft();
        int topCell = tableRegion.getTop();
        IGridRegion headerRegion = grid.getCell(leftCell, topCell).getAbsoluteRegion();

        ArrayList<IUndoableGridTableAction> actions = new ArrayList<>();

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
        for (int prpCell = leftCell + 1; prpCell < leftCell + regionWidth; prpCell++) {
            actions.add(new UndoableClearAction(prpCell, headerRegion.getBottom() + 1, metaInfoWriter));
            /*
             * actions.add(new SetBorderStyleAction(prpCell, headerRegion.getBottom() + 1, makeNewPropStyle(grid,
             * prpCell, headerRegion.getBottom() + 1, prpCell, regionWidth, null) ));
             */
        }

        if (regionWidth >= 3) {
            // set cell style
            // leftCell + 2 - this is index of last property column
            for (int j = leftCell + 2; j < leftCell + regionWidth; j++) {
                actions.add(new SetBorderStyleAction(j,
                    headerRegion.getBottom() + 1,
                    makeNewPropStyle(grid, j, headerRegion.getBottom() + 1, leftCell, regionWidth),
                    metaInfoWriter));
            }
        } else {
            // expand table by including neighboring cell in merged
            // regions, width will equal 3
            int propSize = 3;

            actions
                .add(new MergeCellsAction(new GridRegion(topCell, leftCell, headerRegion.getBottom(), leftCell + 2)));

            // add style for expanded header's and properties's cells
            for (int row = topCell; row < tableRegion.getBottom(); row++) {
                for (int j = leftCell + regionWidth; j < leftCell + 3; j++) {
                    actions.add(new SetBorderStyleAction(j,
                        row,
                        grid.getCell(leftCell + regionWidth - 1, row).getStyle(),
                        metaInfoWriter));
                }
            }

            // add style for expanded others cells
            for (int row = topCell + 1; row < tableRegion.getBottom(); row++) {
                for (int j = leftCell + regionWidth; j < leftCell + 3; j++) {
                    actions.add(new SetBorderStyleAction(j,
                        row + 1,
                        grid.getCell(leftCell + regionWidth - 1, row).getStyle(),
                        metaInfoWriter));
                }
            }

            // merge right cells in each row
            IGridRegion cellToExpandRegion;
            for (int row = headerRegion.getBottom() + 1; row < tableRegion
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

    private static CellStyle makeNewPropStyle(IGrid grid, int col, int row, int regionLeftCell, int regionWidth) {
        ICell cell = grid.getCell(col, row);
        CellStyle newCellStyle = new CellStyle(cell.getStyle());

        ICellStyle cellStyle = cell.getStyle();
        BorderStyle[] borderStyle = cellStyle != null ? cellStyle.getBorderStyle() : null;

        /* Create new cell style */

        if (borderStyle != null && col == regionLeftCell) {
            // Only left border will be set
            if (borderStyle.length == 4) {
                borderStyle = new BorderStyle[] { BorderStyle.NONE,
                        BorderStyle.NONE,
                        BorderStyle.NONE,
                        borderStyle[3] };
            }
        } else if (borderStyle != null && (col - regionLeftCell == regionWidth - 1)) {
            // Only right border will be set
            if (borderStyle.length == 4) {
                borderStyle = new BorderStyle[] { BorderStyle.NONE,
                        borderStyle[1],
                        BorderStyle.NONE,
                        BorderStyle.NONE };
                /*
                 * FIXME add bottom border for expender row (only for last) if (actionType != null && actionType ==
                 * ActionType.EXPAND) { borderStyle = new short[]{CellStyle.BORDER_NONE, borderStyle[1], borderStyle[2],
                 * CellStyle.BORDER_NONE}; } else { borderStyle = new short[]{CellStyle.BORDER_NONE, borderStyle[1],
                 * CellStyle.BORDER_NONE, CellStyle.BORDER_NONE}; }
                 */
            }
        } else {
            borderStyle = new BorderStyle[] { BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE };
        }

        newCellStyle.setBorderStyle(borderStyle);

        return newCellStyle;
    }

    private static IUndoableGridTableAction resizePropertiesHeader(IGridRegion tableRegion,
            IGrid grid,
            MetaInfoWriter metaInfoWriter) {
        int leftCell = tableRegion.getLeft();
        int topCell = tableRegion.getTop();
        int firstPropertyRow = IGridRegion.Tool.height(grid.getCell(leftCell, topCell).getAbsoluteRegion());

        int propsCount = grid.getCell(leftCell, topCell + firstPropertyRow).getHeight();
        if (propsCount == 1) {
            IGridRegion propHeaderRegion = grid.getRegionContaining(leftCell, topCell + firstPropertyRow);
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

    private static boolean tableWithoutPropertySection(String propsHeader) {
        boolean containsPropSection = false;
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
        List<IUndoableGridTableAction> clearActions = new ArrayList<>();
        for (int i = startColumn; i < startColumn + nCols; i++) {
            for (int j = startRow; j < startRow + nRows; j++) {
                if (!grid.isPartOfTheMergedRegion(i, j) || (grid.isTopLeftCellInMergedRegion(i, j))) {
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
        ArrayList<IUndoableGridTableAction> shiftActions = new ArrayList<>();

        // The first step: clear cells that will be lost after shifting
        // columns(just because we need to restore this cells after UNDO)
        if (isInsert) {
            shiftActions.addAll(clearCells(region.getRight() + 1,
                nCols,
                region.getTop(),
                IGridRegion.Tool.height(region),
                grid,
                metaInfoWriter));
        } else {
            for (int column = startColumn - nCols; column < startColumn; column++) {
                for (int row = region.getTop(); row <= region.getBottom(); row++) {
                    if (!grid.isPartOfTheMergedRegion(column, row) || (grid.isTopLeftCellInMergedRegion(column,
                        row) && IGridRegion.Tool.width(grid.getRegionStartingAt(column, row)) <= nCols)) {
                        // Sense of the second check: if it was a merged
                        // cell then it can be removed or resized depending
                        // on count of columns deleted
                        shiftActions.add(new UndoableClearAction(column, row, metaInfoWriter));
                    }
                }
            }
        }

        // The second step: shift cells
        int direction, colFromCopy, colToCopy;
        if (isInsert) {// shift columns left
            direction = -1;
            colFromCopy = region.getRight();
        } else {// shift columns right
            direction = 1;
            colFromCopy = startColumn;
        }
        int numColumnsToBeShifted = region.getRight() - startColumn;
        for (int i = 0; i <= numColumnsToBeShifted; i++) {
            colToCopy = colFromCopy - direction * nCols;
            // from bottom to top, it is made for copying non_top_left cells
            // of merged before the topleft cell of merged region
            for (int row = region.getBottom(); row >= region.getTop(); row--) {
                shiftActions.add(shiftCell(colFromCopy, row, colToCopy, row, grid, metaInfoWriter));
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
     * @param metaInfoWriter class needed to save meta info modifications
     */
    private static List<IUndoableGridTableAction> shiftRows(int startRow,
            int nRows,
            boolean isInsert,
            IGridRegion region,
            IGrid grid,
            MetaInfoWriter metaInfoWriter) {
        ArrayList<IUndoableGridTableAction> shiftActions = new ArrayList<>();

        // The first step: clear cells that will be lost after shifting
        // rows(just because we need to restore this cells after UNDO)
        if (isInsert) {
            shiftActions.addAll(clearCells(region
                .getLeft(), IGridRegion.Tool.width(region), region.getBottom() + 1, nRows, grid, metaInfoWriter));
        } else {
            for (int row = startRow - nRows; row < startRow; row++) {
                for (int column = region.getLeft(); column <= region.getRight(); column++) {
                    if (!grid.isPartOfTheMergedRegion(column, row) || (grid.isTopLeftCellInMergedRegion(column,
                        row) && IGridRegion.Tool.height(grid.getRegionStartingAt(column, row)) <= nRows)) {
                        // Sense of the second check: if it was a merged
                        // cell then it can be removed or resized depending
                        // on count of rows deleted
                        shiftActions.add(new UndoableClearAction(column, row, metaInfoWriter));
                    }
                }
            }
        }

        // The second step: shift cells
        int direction, rowFromCopy;
        if (isInsert) {// shift rows down
            direction = -1;
            rowFromCopy = region.getBottom(); // we gets the bottom row from the region, and are
            // going to shift it down.
        } else {// shift rows up
            direction = 1;
            rowFromCopy = startRow; // we gets the startRow and are
            // going to shift it up.
        }
        int numRowsToBeShifted = region.getBottom() - startRow;
        for (int i = 0; i <= numRowsToBeShifted; i++) {
            int rowToCopy = rowFromCopy - direction * nRows; // compute to which row we need to shift.
            // from right to left, it is made for copying non_top_left cells
            // of merged before the topleft cell of merged region
            for (int column = region.getRight(); column >= region.getLeft(); column--) {
                shiftActions.add(shiftCell(column, rowFromCopy, column, rowToCopy, grid, metaInfoWriter));
            }
            rowFromCopy += direction;
        }
        return shiftActions;
    }

    public static IUndoableGridTableAction removeColumns(int nCols,
            int startColumn,
            IGridRegion region,
            IGrid grid,
            MetaInfoWriter metaInfoWriter) {
        int firstToMove = region.getLeft() + startColumn + nCols;
        int w = IGridRegion.Tool.width(region);
        int h = IGridRegion.Tool.height(region);

        ArrayList<IUndoableGridTableAction> actions = new ArrayList<>(h * (w - startColumn));

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
        int w = IGridRegion.Tool.width(region);
        int h = IGridRegion.Tool.height(region);
        int firstToMove = region.getTop() + startRow + nRows;

        ArrayList<IUndoableGridTableAction> actions = new ArrayList<>(w * (h - startRow));

        // resize merged regions -> shift cells by row -> clear cells
        actions.addAll(resizeMergedRegions(grid, startRow, nRows, REMOVE, ROWS, region, metaInfoWriter));
        actions.addAll(shiftRows(firstToMove, nRows, REMOVE, region, grid, metaInfoWriter));
        actions.addAll(clearCells(region.getLeft(), w, region.getBottom() + 1 - nRows, nRows, grid, metaInfoWriter));

        return new UndoableCompositeAction(actions);
    }
}
