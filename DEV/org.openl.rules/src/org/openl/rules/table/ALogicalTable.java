package org.openl.rules.table;

/**
 * Default implementation for logical tables.
 * 
 * @author Andrei Astrouski
 */
public abstract class ALogicalTable implements ILogicalTable {

    private IGridTable table;

    public ALogicalTable(IGridTable table) {
        this.table = table;
    }

    @Override
    public IGridTable getSource() {
        return table;
    }

    @Override
    public ILogicalTable getColumn(int column) {
        return getSubtable(column, 0, 1, getHeight());
    }

    @Override
    public ILogicalTable getColumns(int from) {
        return getColumns(from, getWidth() - 1);
    }

    @Override
    public ILogicalTable getColumns(int from, int to) {
        int colsNum = to - from + 1;
        return getSubtable(from, 0, colsNum, getHeight());
    }

    @Override
    public ILogicalTable getRow(int row) {
        return getSubtable(0, row, getWidth(), 1);
    }

    @Override
    public ILogicalTable getRows(int from) {
        return getRows(from, getHeight() - 1);
    }

    @Override
    public ILogicalTable getRows(int from, int to) {
        int rowsNum = to - from + 1;
        return getSubtable(0, from, getWidth(), rowsNum);
    }

    @Override
    public ILogicalTable transpose() {
        return LogicalTableHelper.logicalTable(getSource().transpose());
    }

    @Override
    public boolean isNormalOrientation() {        
        return getSource().isNormalOrientation();
    }

    @Override
    public String toString() {
        StringBuilder tableVisualization = new StringBuilder();     
        tableVisualization.append(super.toString())
                .append("(")
                .append(getWidth())
                .append(" x ")
                .append(getHeight())
                .append(")")
                .append("\n");
        
        int height = getHeight();
        int width = getWidth();
        if (width > 0) {
            // Include height of merged cells
            for (int i = 0; i < height; i++) {
                height += getSource().getCell(0, i).getHeight() - 1;
            }
        }
        if (height > 0) {
            // Include width of merged cells
            for (int j = 0; j < width; j++) {
                width += getSource().getCell(j, 0).getWidth() - 1;
            }
        }
        for (int i = 0; i < height; i++) {
            int length = 0;
            for (int j = 0; j < width; j++) {
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
