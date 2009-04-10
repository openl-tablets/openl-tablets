/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 * Table based on Grid coordinates
 *
 */
public interface IGridTable extends ILogicalTable {

    public static final boolean ORIENTATION_NORMAL = true, ORIENTATION_TRANSPOSED = false;

    /**
     *
     * @param col
     * @param row
     * @return
     */
    int getCellHeight(int col, int row);

    ICellInfo getCellInfo(int column, int row);

    /**
     * @param j
     * @param i
     * @return
     */
    ICellStyle getCellStyle(int col, int row);

    int getCellWidth(int col, int row);

    IGrid getGrid();

    int getGridColumn(int column, int row);

    int getGridHeight();

    int getGridRow(int column, int row);

    int getGridWidth();

    /**
     * @param first
     * @param rrow
     * @return
     */
    Object getObjectValue(int first, int rrow);

    /**
     *
     * @return
     */
    IGridRegion getRegion();

    String getStringValue(int col, int row);

    String getUri();

    String getUri(int col, int row);

    boolean isNormalOrientation();

    public boolean isPartOfTheMergedRegion(int column, int row);

}
