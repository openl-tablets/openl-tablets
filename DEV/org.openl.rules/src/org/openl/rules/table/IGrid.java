/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 * 
 * The class representing absolute grid starting at 0,0
 * 
 */
public interface IGrid {

    int CELL_TYPE_NUMERIC = 0;

    int CELL_TYPE_STRING = 1;

    int CELL_TYPE_FORMULA = 2;

    int CELL_TYPE_BLANK = 3;

    int CELL_TYPE_BOOLEAN = 4;

    int CELL_TYPE_ERROR = 5;

    IGridTable[] getTables();

    ICell getCell(int column, int row);

    int getColumnWidth(int col);

    int getMaxColumnIndex(int row);

    int getMaxRowIndex();

    IGridRegion getMergedRegion(int i);

    int getMinColumnIndex(int row);

    int getMinRowIndex();

    int getNumberOfMergedRegions();

    String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd);

    IGridRegion getRegionStartingAt(int colFrom, int rowFrom);

    String getUri();

    boolean isEmpty(int col, int row);

    boolean isPartOfTheMergedRegion(int col, int row);

    boolean isTopLeftCellInMergedRegion(int column, int row);

    boolean isInOneMergedRegion(int firstCellColumn, int firstCellRow, int secondCellColumn, int secondCellRow);

    /**
     * @return Merged region containing cell specified by coordinates or <code>null</code>
     */
    IGridRegion getRegionContaining(int column, int row);
}
