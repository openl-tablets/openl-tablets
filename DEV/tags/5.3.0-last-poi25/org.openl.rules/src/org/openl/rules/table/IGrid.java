/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellStyle;

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

    public final static int CELL_TYPE_NUMERIC = 0;

    public final static int CELL_TYPE_STRING = 1;

    public final static int CELL_TYPE_FORMULA = 2;

    public final static int CELL_TYPE_BLANK = 3;

    public final static int CELL_TYPE_BOOLEAN = 4;

    public final static int CELL_TYPE_ERROR = 5;

    // This type is not supported by HSSF, so it can be used only for read-only
    // grids (filters)
    // May be make it not type but subtype

    // public final static int CELL_SUB_TYPE_URL = 11;
    // public final static int CELL_SUB_TYPE_DATE = 12;

    /**
     * Calculates cell height: if it is left top corner of merged region -
     * returns region height, else - returns 1.
     */
    int getCellHeight(int column, int row);

    ICellInfo getCellInfo(int column, int row);

    ICellStyle getCellStyle(int column, int row);

    int getCellType(int column, int row);

    String getCellUri(int column, int row);

    /**
     * @see #getCellHeight
     */
    int getCellWidth(int column, int row);

    int getColumnWidth(int col);

    Date getDateCellValue(int column, int row);

    double getDoubleCellValue(int column, int row);

    FormattedCell getFormattedCell(int column, int row);

    String getFormattedCellValue(int column, int row);

    int getMaxColumnIndex(int row);

    int getMaxRowIndex();

    IGridRegion getMergedRegion(int i);

    int getMinColumnIndex(int row);

    int getMinRowIndex();

    int getNumberOfMergedRegions();

    Object getObjectCellValue(int column, int row);

    String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd);

    /**
     * @param colFrom
     * @param rowFrom
     * @return
     */
    IGridRegion getRegionStartingAt(int colFrom, int rowFrom);

    String getStringCellValue(int column, int row);

    String getUri();

    boolean isEmpty(int col, int row);

    boolean isPartOfTheMergedRegion(int col, int row);

}
