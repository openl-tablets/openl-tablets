package org.openl.rules.table;

/**
 * Default implementation for logical tables.
 * 
 * @author Andrei Astrouski
 */
public abstract class ALogicalTable implements ILogicalTable {

    protected IGridTable table;

    public ALogicalTable(IGridTable table) {
        this.table = table;
    }

    public IGridTable getSource() {
        return table;
    }

    public ILogicalTable getColumn(int column) {
        return getSubtable(column, 0, 1, getHeight());
    }

    public ILogicalTable getColumns(int from) {
        return getColumns(from, getWidth() - 1);
    }

    public ILogicalTable getColumns(int from, int to) {
        int colsNum = to - from + 1;
        return getSubtable(from, 0, colsNum, getHeight());
    }

    public ILogicalTable getRow(int row) {
        return getSubtable(0, row, getWidth(), 1);
    }

    public ILogicalTable getRows(int from) {
        return getRows(from, getHeight() - 1);
    }

    public ILogicalTable getRows(int from, int to) {
        int rowsNum = to - from + 1;
        return getSubtable(0, from, getWidth(), rowsNum);
    }

    public ILogicalTable transpose() {
        return LogicalTableHelper.logicalTable(getSource().transpose());
    }

    public boolean isNormalOrientation() {        
        return getSource().isNormalOrientation();
    }

    public ICell getCell(int column, int row) {
        return null;
    }

    @Override
    public String toString() {
        StringBuffer tableVisualization = new StringBuffer();     
        tableVisualization.append(super.toString()).append("\n");
        
        for (int i = 0; i < getHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getWidth(); j++) {
                String stringValue = getSource().getCell(j, i).getStringValue();
                if (stringValue == null) {
                    stringValue = "EMPTY";
                }
                length += stringValue.length();
                tableVisualization.append(stringValue);                
                tableVisualization.append("|");
            }
            tableVisualization.append("\n");
            for(int k = 0; k <= length; k++) {
                tableVisualization.append("-");
            }   
            tableVisualization.append("\n");
        }
        
        return  tableVisualization.toString();
    }

}
