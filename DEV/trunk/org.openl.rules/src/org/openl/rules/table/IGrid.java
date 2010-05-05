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

    /**
     * Same as in HSSFCell - no conversion requires for POI
     */

    int CELL_TYPE_NUMERIC = 0;

    int CELL_TYPE_STRING = 1;

    int CELL_TYPE_FORMULA = 2;

    int CELL_TYPE_BLANK = 3;

    int CELL_TYPE_BOOLEAN = 4;

    int CELL_TYPE_ERROR = 5;

    // This type is not supported by HSSF, so it can be used only for read-only
    // grids (filters)
    // May be make it not type but subtype

    // public final static int CELL_SUB_TYPE_URL = 11;
    // public final static int CELL_SUB_TYPE_DATE = 12;

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

    /**
     * @return Merged region containing cell specified by coordinates or <code>null</code>
     */
    IGridRegion getRegionContaining(int column, int row);
}
