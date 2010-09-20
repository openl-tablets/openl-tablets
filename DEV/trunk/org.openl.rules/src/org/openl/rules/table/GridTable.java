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
public class GridTable extends AGridTable {

    private IGridRegion region;
    private IGrid grid;

    public GridTable(IGridRegion reg, IGrid grid) {
        this.region = reg;
        this.grid = grid;
    }

    public GridTable(int top, int left, int bottom, int right, IGrid grid) {
        this.region = new GridRegion(top, left, bottom, right);
        this.grid = grid;
    }

    public int getBottom() {
        return region.getBottom();
    }

    public IGrid getGrid() {
        return grid;
    }

    public int getGridColumn(int column, int row) {
        return region.getLeft() + column;
    }

    public int getGridHeight() {
        return region.getBottom() - region.getTop() + 1;
    }

    public int getGridRow(int column, int row) {
        return region.getTop() + row;
    }

    public int getGridWidth() {
        return region.getRight() - region.getLeft() + 1;
    }

    public int getLeft() {
        return region.getLeft();
    }

    @Override
    public IGridRegion getRegion() {
        return region;
    }

    public int getRight() {
        return region.getRight();
    }

    public int getTop() {
        return region.getTop();
    }

    public boolean isNormalOrientation() {
        return true;
    }

    @Override
    public String toString() { 
        StringBuffer tableVizualization = new StringBuffer();
        tableVizualization.append("G[" + getTop() + "," + getLeft() + "," + getBottom() + "," + getRight() + "]\n");
        for (int i = 0; i < getGridHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getGridWidth(); j++) {
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
