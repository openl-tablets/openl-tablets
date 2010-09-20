/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;


/**
 * @author snshor
 *
 * Table based on Grid coordinates
 *
 */
public interface IGridTable extends IGridTableOperations {

    boolean ORIENTATION_NORMAL = true;
    boolean ORIENTATION_TRANSPOSED = false;

    ICell getCell(int column, int row);

    IGrid getGrid();

    int getGridColumn(int column, int row);    

    int getGridRow(int column, int row);

    IGridRegion getRegion();

    String getUri();

    String getUri(int col, int row);

    boolean isNormalOrientation();

    public boolean isPartOfTheMergedRegion(int column, int row);
    
    int getGridHeight();
    
    int getGridWidth();
    
    /**
     * @return underlying grid table
     */
    IGridTable getGridTable();

}
