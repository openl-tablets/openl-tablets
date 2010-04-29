/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class GridTable extends AGridTable implements IGridRegion {

    private IGrid grid;

    private int top;
    
    private int left; 
    
    private int right;
    
    private int bottom;

    public GridTable(IGridRegion reg, IGrid grid) {
        top = reg.getTop();
        left = reg.getLeft();
        bottom = reg.getBottom();
        right = reg.getRight();
        this.grid = grid;
    }

    public GridTable(int top, int left, int bottom, int right, IGrid grid) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.grid = grid;
    }

    public int getBottom() {
        return bottom;
    }

    public IGrid getGrid() {
        return grid;
    }

    public int getGridColumn(int column, int row) {
        return left + column;
    }

    public int getGridHeight() {
        return bottom - top + 1;
    }

    public int getGridRow(int column, int row) {
        return top + row;
    }

    public int getGridWidth() {
        return right - left + 1;
    }

    public int getLeft() {
        return left;
    }

    @Override
    public IGridRegion getRegion() {
        return this;
    }

    public int getRight() {
        return right;
    }

    public int getTop() {
        return top;
    }

    public boolean isNormalOrientation() {
        return true;
    }

    @Override
    public String toString() { 
        StringBuffer tableVizualization = new StringBuffer();
        tableVizualization.append("G[" + getTop() + "," + getLeft() + "," + getBottom() + "," + getRight() + "]\n");
        for (int i = 0; i < getLogicalHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getLogicalWidth(); j++) {
                String strValue = getCell(j, i).getStringValue();
                if (strValue == null) {
                    strValue = "EMPTY";
                }
                length += strValue.length();
                tableVizualization.append(strValue);                
                tableVizualization.append("|");
            }
            tableVizualization.append("\n");
            for(int k = 0; k <= length; k++) {
                tableVizualization.append("-");
            }   
            tableVizualization.append("\n");
        }
        
        return  tableVizualization.toString();
    }    
}
